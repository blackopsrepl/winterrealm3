/*
 * Copyright © 2004-2020 L2J Server
 *
 * This file is part of L2J Server.
 *
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.bot.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.l2j.gameserver.bot.profile.BotCombatPolicy.Targeting;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.AmountType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Comparison;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.EffectState;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Group;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.GroupFilter;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.ResourceType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.StatusType;
import net.sf.l2j.gameserver.bot.profile.BotRuleCondition.Subject;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy.Intent;
import net.sf.l2j.gameserver.bot.profile.BotSkillPolicy.Target;
import net.sf.l2j.gameserver.model.base.ClassId;

/** Strict loader for one-file-per-exact-class behavior policies. */
public final class BotClassProfileLoader
{
	public static final Path DATAPACK_DIRECTORY = Path.of("data", "bots", "profiles", "classes");

	private static final Set<String> ROOT_ATTRIBUTES = Set.of("classId", "name", "schemaVersion");
	private static final Set<String> COMBAT_ATTRIBUTES = Set.of("targeting", "acquisitionRange", "maximumChaseDistance", "preferredRange", "basicAttackPriority");
	private static final Set<String> RELATIONSHIP_ATTRIBUTES = Set.of("classId", "allySupportPriority", "allyProtectionPriority", "hostileAttackPriority", "hostileThreatPriority", "hostileControlPriority", "hostileInterruptPriority");
	private static final Set<String> SKILL_ATTRIBUTES = Set.of("key", "skillId", "intent", "target", "priority", "forceUse", "dontMove");

	/**
	 * Loads every regular XML file in lexical order and indexes exact keys.
	 * @param directory the class-profile directory
	 * @return the immutable exact-key registry
	 * @throws IOException when the directory or a profile cannot be read
	 */
	public BotClassProfileRegistry loadDirectory(Path directory) throws IOException
	{
		requireDirectory(directory);
		List<Path> files;
		try (Stream<Path> entries = Files.list(directory))
		{
			files = entries.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).filter(path -> path.getFileName().toString().endsWith(".xml")).sorted().toList();
		}
		if (files.isEmpty())
		{
			throw new IllegalArgumentException("No exact-class bot profiles found in " + directory);
		}

		List<BotClassProfile> profiles = new ArrayList<>(files.size());
		for (Path file : files)
		{
			profiles.add(load(file));
		}
		return new BotClassProfileRegistry(profiles);
	}

	/** Loads one profile and requires its filename to repeat its exact key. */
	public BotClassProfile load(Path file) throws IOException
	{
		if (file == null)
		{
			throw new IllegalArgumentException("Bot class profile path cannot be null");
		}
		if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
		{
			throw new IllegalArgumentException("Bot class profile is not a regular file: " + file);
		}

		Document document;
		try (InputStream input = Files.newInputStream(file))
		{
			document = newDocumentBuilder().parse(input);
		}
		catch (SAXException e)
		{
			throw invalid(file, "Malformed or unsafe XML", e);
		}

		Element root = document.getDocumentElement();
		if ((root == null) || !"botClassProfile".equals(root.getTagName()))
		{
			throw invalid(file, "Root element must be <botClassProfile>");
		}
		requireAttributes(file, root, ROOT_ATTRIBUTES, Set.of());

		ClassId classId = parseEnum(file, root, "classId", ClassId.class);
		int schemaVersion = parseInt(file, root, "schemaVersion");
		String profileName = requireAttribute(file, root, "name");

		Element combatElement = null;
		Element relationshipsElement = null;
		Element skillsElement = null;
		for (Element child : childElements(file, root))
		{
			switch (child.getTagName())
			{
				case "combat" ->
				{
					if (combatElement != null)
					{
						throw invalid(file, "Profile contains more than one <combat> element");
					}
					combatElement = child;
				}
				case "skills" ->
				{
					if (skillsElement != null)
					{
						throw invalid(file, "Profile contains more than one <skills> element");
					}
					skillsElement = child;
				}
				case "relationships" ->
				{
					if (relationshipsElement != null)
					{
						throw invalid(file, "Profile contains more than one <relationships> element");
					}
					relationshipsElement = child;
				}
				default -> throw invalid(file, "Unknown profile element <" + child.getTagName() + ">");
			}
		}
		if ((combatElement == null) || (relationshipsElement == null) || (skillsElement == null))
		{
			throw invalid(file, "Profile requires exactly one <combat>, one <relationships>, and one <skills> element");
		}

		BotClassProfile profile = new BotClassProfile(schemaVersion, classId, profileName, parseCombat(file, combatElement), parseRelationships(file, relationshipsElement), parseSkills(file, skillsElement));
		String expectedFilename = profile.getClassId().name() + "--" + profile.getProfileName() + ".xml";
		if (!expectedFilename.equals(file.getFileName().toString()))
		{
			throw invalid(file, "Profile filename must be " + expectedFilename);
		}
		return profile;
	}

	private static BotCombatPolicy parseCombat(Path file, Element element)
	{
		requireAttributes(file, element, COMBAT_ATTRIBUTES, Set.of());
		requireNoChildren(file, element);
		return new BotCombatPolicy(parseEnum(file, element, "targeting", Targeting.class), parseInt(file, element, "acquisitionRange"), parseInt(file, element, "maximumChaseDistance"), parseInt(file, element, "preferredRange"), parseInt(file, element, "basicAttackPriority"));
	}

	private static List<BotClassRelationship> parseRelationships(Path file, Element element)
	{
		requireAttributes(file, element, Set.of(), Set.of());
		List<BotClassRelationship> relationships = new ArrayList<>();
		for (Element relationship : childElements(file, element))
		{
			if (!"class".equals(relationship.getTagName()))
			{
				throw invalid(file, "Unknown <relationships> child <" + relationship.getTagName() + ">");
			}
			requireAttributes(file, relationship, RELATIONSHIP_ATTRIBUTES, Set.of());
			requireNoChildren(file, relationship);
			relationships.add(new BotClassRelationship(parseEnum(file, relationship, "classId", ClassId.class), parseInt(file, relationship, "allySupportPriority"), parseInt(file, relationship, "allyProtectionPriority"), parseInt(file, relationship, "hostileAttackPriority"), parseInt(file, relationship, "hostileThreatPriority"), parseInt(file, relationship, "hostileControlPriority"), parseInt(file, relationship, "hostileInterruptPriority")));
		}
		return List.copyOf(relationships);
	}

	private static List<BotSkillPolicy> parseSkills(Path file, Element element)
	{
		requireAttributes(file, element, Set.of(), Set.of());
		List<BotSkillPolicy> skills = new ArrayList<>();
		for (Element skill : childElements(file, element))
		{
			if (!"skill".equals(skill.getTagName()))
			{
				throw invalid(file, "Unknown <skills> child <" + skill.getTagName() + ">");
			}
			requireAttributes(file, skill, SKILL_ATTRIBUTES, Set.of());
			List<Element> children = childElements(file, skill);
			if ((children.size() != 1) || !"when".equals(children.get(0).getTagName()))
			{
				throw invalid(file, "Each <skill> requires exactly one <when> element");
			}
			skills.add(new BotSkillPolicy(requireAttribute(file, skill, "key"), parseInt(file, skill, "skillId"), parseEnum(file, skill, "intent", Intent.class), parseEnum(file, skill, "target", Target.class), parseInt(file, skill, "priority"), parseBoolean(file, skill, "forceUse"), parseBoolean(file, skill, "dontMove"), parseConditions(file, children.get(0))));
		}
		return List.copyOf(skills);
	}

	private static List<BotRuleCondition> parseConditions(Path file, Element element)
	{
		requireAttributes(file, element, Set.of(), Set.of());
		List<BotRuleCondition> conditions = new ArrayList<>();
		for (Element condition : childElements(file, element))
		{
			conditions.add(switch (condition.getTagName())
			{
				case "resource" -> parseResource(file, condition);
				case "distance" -> parseDistance(file, condition);
				case "effect" -> parseEffect(file, condition);
				case "status" -> parseStatus(file, condition);
				case "class" -> parseExactClass(file, condition);
				case "count" -> parseCount(file, condition);
				case "classCount" -> parseClassCount(file, condition);
				default -> throw invalid(file, "Unknown condition element <" + condition.getTagName() + ">");
			});
		}
		if (conditions.isEmpty())
		{
			throw invalid(file, "A <when> element requires at least one condition");
		}
		return List.copyOf(conditions);
	}

	private static BotRuleCondition parseResource(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("subject", "resource", "amount", "comparison", "value"), Set.of());
		requireNoChildren(file, element);
		return new BotRuleCondition.Resource(parseEnum(file, element, "subject", Subject.class), parseEnum(file, element, "resource", ResourceType.class), parseEnum(file, element, "amount", AmountType.class), parseEnum(file, element, "comparison", Comparison.class), parseInt(file, element, "value"));
	}

	private static BotRuleCondition parseDistance(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("subject", "comparison", "value"), Set.of());
		requireNoChildren(file, element);
		return new BotRuleCondition.Distance(parseEnum(file, element, "subject", Subject.class), parseEnum(file, element, "comparison", Comparison.class), parseInt(file, element, "value"));
	}

	private static BotRuleCondition parseEffect(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("subject", "skillId", "state"), Set.of());
		requireNoChildren(file, element);
		return new BotRuleCondition.Effect(parseEnum(file, element, "subject", Subject.class), parseInt(file, element, "skillId"), parseEnum(file, element, "state", EffectState.class));
	}

	private static BotRuleCondition parseStatus(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("subject", "status", "expected"), Set.of());
		requireNoChildren(file, element);
		return new BotRuleCondition.Status(parseEnum(file, element, "subject", Subject.class), parseEnum(file, element, "status", StatusType.class), parseBoolean(file, element, "expected"));
	}

	private static BotRuleCondition parseExactClass(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("subject", "classId", "expected"), Set.of());
		requireNoChildren(file, element);
		return new BotRuleCondition.ExactClass(parseEnum(file, element, "subject", Subject.class), parseEnum(file, element, "classId", ClassId.class), parseBoolean(file, element, "expected"));
	}

	private static BotRuleCondition parseCount(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("group", "filter", "comparison", "value", "radius"), Set.of("threshold"));
		requireNoChildren(file, element);
		int threshold = element.hasAttribute("threshold") ? parseInt(file, element, "threshold") : BotRuleCondition.GroupCount.NO_THRESHOLD;
		return new BotRuleCondition.GroupCount(parseEnum(file, element, "group", Group.class), parseEnum(file, element, "filter", GroupFilter.class), parseEnum(file, element, "comparison", Comparison.class), parseInt(file, element, "value"), threshold, parseInt(file, element, "radius"));
	}

	private static BotRuleCondition parseClassCount(Path file, Element element)
	{
		requireAttributes(file, element, Set.of("group", "classId", "effectSkillIds", "effectState", "comparison", "value", "radius"), Set.of());
		requireNoChildren(file, element);
		return new BotRuleCondition.GroupClassCount(parseEnum(file, element, "group", Group.class), parseEnum(file, element, "classId", ClassId.class), parseIntList(file, element, "effectSkillIds"), parseEnum(file, element, "effectState", EffectState.class), parseEnum(file, element, "comparison", Comparison.class), parseInt(file, element, "value"), parseInt(file, element, "radius"));
	}

	private static void requireDirectory(Path directory)
	{
		if (directory == null)
		{
			throw new IllegalArgumentException("Bot class profile directory cannot be null");
		}
		if (!Files.isDirectory(directory))
		{
			throw new IllegalArgumentException("Bot class profile directory does not exist: " + directory);
		}
	}

	private static DocumentBuilder newDocumentBuilder()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(ThrowingErrorHandler.INSTANCE);
			return builder;
		}
		catch (ParserConfigurationException e)
		{
			throw new IllegalStateException("Could not create a secure bot-profile XML parser", e);
		}
	}

	private static List<Element> childElements(Path file, Element parent)
	{
		List<Element> elements = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for (int index = 0; index < children.getLength(); index++)
		{
			Node child = children.item(index);
			switch (child.getNodeType())
			{
				case Node.ELEMENT_NODE -> elements.add((Element) child);
				case Node.TEXT_NODE, Node.CDATA_SECTION_NODE ->
				{
					if (!child.getTextContent().isBlank())
					{
						throw invalid(file, "Unexpected text inside <" + parent.getTagName() + ">");
					}
				}
				case Node.COMMENT_NODE ->
				{
					// Comments are documentation, not policy data.
				}
				default -> throw invalid(file, "Unsupported XML node inside <" + parent.getTagName() + ">");
			}
		}
		return elements;
	}

	private static void requireNoChildren(Path file, Element element)
	{
		if (!childElements(file, element).isEmpty())
		{
			throw invalid(file, "Element <" + element.getTagName() + "> cannot contain child elements");
		}
	}

	private static void requireAttributes(Path file, Element element, Set<String> required, Set<String> optional)
	{
		Set<String> allowed = new HashSet<>(required);
		allowed.addAll(optional);
		NamedNodeMap attributes = element.getAttributes();
		for (int index = 0; index < attributes.getLength(); index++)
		{
			String name = attributes.item(index).getNodeName();
			if (!allowed.contains(name))
			{
				throw invalid(file, "Unknown attribute " + name + " on <" + element.getTagName() + ">");
			}
		}
		for (String name : required)
		{
			requireAttribute(file, element, name);
		}
	}

	private static String requireAttribute(Path file, Element element, String name)
	{
		if (!element.hasAttribute(name) || element.getAttribute(name).isBlank())
		{
			throw invalid(file, "Missing attribute " + name + " on <" + element.getTagName() + ">");
		}
		return element.getAttribute(name).trim();
	}

	private static int parseInt(Path file, Element element, String name)
	{
		String value = requireAttribute(file, element, name);
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			throw invalid(file, "Attribute " + name + " on <" + element.getTagName() + "> must be an integer", e);
		}
	}

	private static List<Integer> parseIntList(Path file, Element element, String name)
	{
		String value = requireAttribute(file, element, name);
		List<Integer> values = new ArrayList<>();
		for (String component : value.split(",", -1))
		{
			if (component.isBlank())
			{
				throw invalid(file, "Attribute " + name + " on <" + element.getTagName() + "> must be a comma-separated integer list");
			}
			try
			{
				values.add(Integer.parseInt(component.trim()));
			}
			catch (NumberFormatException e)
			{
				throw invalid(file, "Attribute " + name + " on <" + element.getTagName() + "> must be a comma-separated integer list", e);
			}
		}
		return List.copyOf(values);
	}

	private static boolean parseBoolean(Path file, Element element, String name)
	{
		return switch (requireAttribute(file, element, name).toLowerCase(Locale.ROOT))
		{
			case "true" -> true;
			case "false" -> false;
			default -> throw invalid(file, "Attribute " + name + " on <" + element.getTagName() + "> must be true or false");
		};
	}

	private static <E extends Enum<E>> E parseEnum(Path file, Element element, String name, Class<E> type)
	{
		String value = requireAttribute(file, element, name);
		try
		{
			return Enum.valueOf(type, value);
		}
		catch (IllegalArgumentException e)
		{
			throw invalid(file, "Unknown " + name + " value " + value + " on <" + element.getTagName() + ">", e);
		}
	}

	private static IllegalArgumentException invalid(Path file, String message)
	{
		return new IllegalArgumentException("Invalid bot class profile " + file + ": " + message);
	}

	private static IllegalArgumentException invalid(Path file, String message, Exception cause)
	{
		return new IllegalArgumentException("Invalid bot class profile " + file + ": " + message, cause);
	}

	private static final class ThrowingErrorHandler implements ErrorHandler
	{
		private static final ThrowingErrorHandler INSTANCE = new ThrowingErrorHandler();

		@Override
		public void warning(SAXParseException exception) throws SAXException
		{
			throw exception;
		}

		@Override
		public void error(SAXParseException exception) throws SAXException
		{
			throw exception;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException
		{
			throw exception;
		}
	}
}
