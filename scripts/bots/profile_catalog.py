"""Explicit authoring catalog for native exact-class player-bot profiles.

This module is deliberately data, not a runtime behavior hierarchy. Every bot
profile and every observed or supported character is named by its exact active
ClassId. Shared tuples below only reduce authoring repetition; the compiler
emits independent exact-recipient policies and the game server has no role,
race, parent-class, or mage/fighter fallback.
"""

from typing import NamedTuple

from class_relationship_catalog import EXACT_CLASS_RELATIONSHIP_MATRICES

ALL_PLAYABLE_CLASSES = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"warlock",
	"cleric",
	"bishop",
	"prophet",
	"elvenFighter",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"elementalSummoner",
	"oracle",
	"elder",
	"darkFighter",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"phantomSummoner",
	"shillienOracle",
	"shillenElder",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"sagittarius",
	"adventurer",
	"archmage",
	"soultaker",
	"arcanaLord",
	"cardinal",
	"hierophant",
	"evaTemplar",
	"swordMuse",
	"windRider",
	"moonlightSentinel",
	"mysticMuse",
	"elementalMaster",
	"evaSaint",
	"shillienTemplar",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"stormScreamer",
	"spectralMaster",
	"shillienSaint",
	"titan",
	"grandKhauatari",
	"dominator",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

# These exact classes receive physical weapon support from a Hierophant.
MIGHT_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"prophet",
	"elvenFighter",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"darkFighter",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"sagittarius",
	"adventurer",
	"hierophant",
	"evaTemplar",
	"swordMuse",
	"windRider",
	"moonlightSentinel",
	"shillienTemplar",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"titan",
	"grandKhauatari",
	"dominator",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

# These exact classes receive casting and maximum-MP support.
CASTING_RECIPIENTS = (
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"warlock",
	"cleric",
	"bishop",
	"prophet",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"elementalSummoner",
	"oracle",
	"elder",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"phantomSummoner",
	"shillienOracle",
	"shillenElder",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"archmage",
	"soultaker",
	"arcanaLord",
	"cardinal",
	"hierophant",
	"mysticMuse",
	"elementalMaster",
	"evaSaint",
	"stormScreamer",
	"spectralMaster",
	"shillienSaint",
	"dominator",
	"doomcryer",
)

# Greater Might and Greater Shield are mutually exclusive in Interlude. These
# two explicit lists form a checked partition of every playable exact class.
GREATER_MIGHT_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"elvenFighter",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"darkFighter",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"sagittarius",
	"adventurer",
	"windRider",
	"moonlightSentinel",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"titan",
	"grandKhauatari",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

GREATER_SHIELD_RECIPIENTS = (
	"knight",
	"paladin",
	"darkAvenger",
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"warlock",
	"cleric",
	"bishop",
	"prophet",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"elementalSummoner",
	"oracle",
	"elder",
	"palusKnight",
	"shillienKnight",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"phantomSummoner",
	"shillienOracle",
	"shillenElder",
	"orcMage",
	"orcShaman",
	"overlord",
	"phoenixKnight",
	"hellKnight",
	"archmage",
	"soultaker",
	"arcanaLord",
	"cardinal",
	"hierophant",
	"evaTemplar",
	"swordMuse",
	"mysticMuse",
	"elementalMaster",
	"evaSaint",
	"shillienTemplar",
	"stormScreamer",
	"spectralMaster",
	"shillienSaint",
	"dominator",
)

BERSERKER_SPIRIT_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"elvenFighter",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"darkFighter",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"sagittarius",
	"adventurer",
	"archmage",
	"soultaker",
	"windRider",
	"moonlightSentinel",
	"mysticMuse",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"stormScreamer",
	"titan",
	"grandKhauatari",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

PROPHECY_OF_FIRE_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"hawkeye",
	"elvenFighter",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"silverRanger",
	"darkFighter",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"phantomRanger",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"sagittarius",
	"evaTemplar",
	"swordMuse",
	"moonlightSentinel",
	"shillienTemplar",
	"spectralDancer",
	"ghostSentinel",
	"titan",
	"grandKhauatari",
	"dominator",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

SHIELD_BLOCK_RECIPIENTS = (
	"knight",
	"paladin",
	"darkAvenger",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"palusKnight",
	"shillienKnight",
	"phoenixKnight",
	"hellKnight",
	"evaTemplar",
	"swordMuse",
	"shillienTemplar",
)

AGILITY_RECIPIENTS = (
	"rogue",
	"treasureHunter",
	"hawkeye",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"orcMonk",
	"tyrant",
	"sagittarius",
	"adventurer",
	"windRider",
	"moonlightSentinel",
	"ghostHunter",
	"ghostSentinel",
	"grandKhauatari",
)

DECREASE_WEIGHT_RECIPIENTS = (
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"fortuneSeeker",
	"maestro",
)

WILD_MAGIC_RECIPIENTS = (
	"wizard",
	"sorceror",
	"necromancer",
	"elvenWizard",
	"spellsinger",
	"darkWizard",
	"spellhowler",
	"archmage",
	"soultaker",
	"mysticMuse",
	"stormScreamer",
)

EMPOWER_RECIPIENTS = (
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"warlock",
	"cleric",
	"bishop",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"elementalSummoner",
	"oracle",
	"elder",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"phantomSummoner",
	"shillienOracle",
	"shillenElder",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"archmage",
	"soultaker",
	"arcanaLord",
	"cardinal",
	"mysticMuse",
	"elementalMaster",
	"evaSaint",
	"stormScreamer",
	"spectralMaster",
	"shillienSaint",
	"dominator",
	"doomcryer",
)

VAMPIRIC_RAGE_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"treasureHunter",
	"elvenFighter",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"plainsWalker",
	"darkFighter",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"adventurer",
	"evaTemplar",
	"swordMuse",
	"windRider",
	"shillienTemplar",
	"spectralDancer",
	"ghostHunter",
	"titan",
	"grandKhauatari",
	"dominator",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

PROPHECY_OF_WATER_RECIPIENTS = (
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"warlock",
	"cleric",
	"bishop",
	"prophet",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"elementalSummoner",
	"oracle",
	"elder",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"phantomSummoner",
	"shillienOracle",
	"shillenElder",
	"archmage",
	"soultaker",
	"arcanaLord",
	"cardinal",
	"hierophant",
	"mysticMuse",
	"elementalMaster",
	"evaSaint",
	"stormScreamer",
	"spectralMaster",
	"shillienSaint",
)

# Doomcryer's mutually exclusive third-class chants use a checked exact-class
# partition. Magic-oriented recipients receive Magnus' Chant; every remaining
# exact class receives Chant of Victory.
MAGNUS_CHANT_RECIPIENTS = PROPHECY_OF_WATER_RECIPIENTS

CHANT_OF_VICTORY_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"elvenFighter",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"darkFighter",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"sagittarius",
	"adventurer",
	"evaTemplar",
	"swordMuse",
	"windRider",
	"moonlightSentinel",
	"shillienTemplar",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"titan",
	"grandKhauatari",
	"dominator",
	"doomcryer",
	"fortuneSeeker",
	"maestro",
)

# Prophecy of Wind is reserved for exact dagger and archer progressions. Other
# physical classes retain access to Prophecy of Fire or Chant of Victory rather
# than being locked into the first mutually exclusive prophecy cast.
PROPHECY_OF_WIND_RECIPIENTS = (
	"rogue",
	"treasureHunter",
	"hawkeye",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"sagittarius",
	"adventurer",
	"windRider",
	"moonlightSentinel",
	"ghostHunter",
	"ghostSentinel",
)

# Recharge's Interlude skill condition excludes the six exact recharger
# classes. Every other playable class appears in exactly one urgency tier.
RECHARGE_PRIORITY_RECIPIENTS = (
	"mage",
	"wizard",
	"sorceror",
	"necromancer",
	"warlock",
	"cleric",
	"bishop",
	"prophet",
	"elvenMage",
	"elvenWizard",
	"spellsinger",
	"elementalSummoner",
	"darkMage",
	"darkWizard",
	"spellhowler",
	"phantomSummoner",
	"orcMage",
	"orcShaman",
	"overlord",
	"warcryer",
	"archmage",
	"soultaker",
	"arcanaLord",
	"cardinal",
	"hierophant",
	"mysticMuse",
	"elementalMaster",
	"stormScreamer",
	"spectralMaster",
	"dominator",
	"doomcryer",
)

RECHARGE_SKILL_RECIPIENTS = (
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"sagittarius",
	"adventurer",
	"evaTemplar",
	"swordMuse",
	"windRider",
	"moonlightSentinel",
	"shillienTemplar",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"titan",
	"grandKhauatari",
	"fortuneSeeker",
	"maestro",
)

RECHARGE_RESERVE_RECIPIENTS = (
	"fighter",
	"elvenFighter",
	"darkFighter",
	"orcFighter",
	"dwarvenFighter",
)

RECHARGE_FORBIDDEN_RECIPIENTS = (
	"oracle",
	"elder",
	"evaSaint",
	"shillienOracle",
	"shillenElder",
	"shillienSaint",
)


class HealRecipientPolicy(NamedTuple):
	"""One exact recipient's authored duty and HP intervention thresholds."""

	duty_priority: int
	battle_threshold: int
	major_threshold: int
	greater_threshold: int


# These are independent exact-class decisions. Repeated values are deliberate;
# no role name or tuple is emitted to or interpreted by the game server.
HEAL_RECIPIENT_POLICIES = {
	"fighter": HealRecipientPolicy(2000, 32, 58, 72),
	"warrior": HealRecipientPolicy(2500, 35, 62, 76),
	"gladiator": HealRecipientPolicy(2600, 36, 63, 77),
	"warlord": HealRecipientPolicy(3100, 40, 70, 84),
	"knight": HealRecipientPolicy(4000, 42, 72, 86),
	"paladin": HealRecipientPolicy(4100, 42, 73, 87),
	"darkAvenger": HealRecipientPolicy(4200, 44, 75, 89),
	"rogue": HealRecipientPolicy(1500, 28, 52, 66),
	"treasureHunter": HealRecipientPolicy(1550, 27, 50, 64),
	"hawkeye": HealRecipientPolicy(1900, 30, 55, 70),
	"mage": HealRecipientPolicy(1900, 35, 60, 75),
	"wizard": HealRecipientPolicy(1950, 35, 60, 75),
	"sorceror": HealRecipientPolicy(2000, 36, 61, 76),
	"necromancer": HealRecipientPolicy(2200, 37, 63, 78),
	"warlock": HealRecipientPolicy(2350, 37, 64, 79),
	"cleric": HealRecipientPolicy(3500, 42, 68, 82),
	"bishop": HealRecipientPolicy(3700, 45, 72, 86),
	"prophet": HealRecipientPolicy(3300, 40, 66, 80),
	"elvenFighter": HealRecipientPolicy(2000, 32, 58, 72),
	"elvenKnight": HealRecipientPolicy(4000, 42, 72, 86),
	"templeKnight": HealRecipientPolicy(4150, 43, 74, 88),
	"swordSinger": HealRecipientPolicy(3100, 39, 68, 82),
	"elvenScout": HealRecipientPolicy(1500, 27, 50, 64),
	"plainsWalker": HealRecipientPolicy(1550, 26, 49, 63),
	"silverRanger": HealRecipientPolicy(1900, 30, 55, 70),
	"elvenMage": HealRecipientPolicy(1900, 35, 60, 75),
	"elvenWizard": HealRecipientPolicy(1950, 35, 60, 75),
	"spellsinger": HealRecipientPolicy(2000, 36, 61, 76),
	"elementalSummoner": HealRecipientPolicy(2350, 37, 64, 79),
	"oracle": HealRecipientPolicy(3500, 42, 68, 82),
	"elder": HealRecipientPolicy(3700, 45, 72, 86),
	"darkFighter": HealRecipientPolicy(2050, 33, 59, 73),
	"palusKnight": HealRecipientPolicy(4050, 43, 73, 87),
	"shillienKnight": HealRecipientPolicy(4200, 44, 75, 89),
	"bladedancer": HealRecipientPolicy(3150, 39, 68, 82),
	"assassin": HealRecipientPolicy(1500, 27, 50, 64),
	"abyssWalker": HealRecipientPolicy(1550, 25, 48, 62),
	"phantomRanger": HealRecipientPolicy(1900, 30, 55, 70),
	"darkMage": HealRecipientPolicy(1900, 35, 60, 75),
	"darkWizard": HealRecipientPolicy(1950, 35, 60, 75),
	"spellhowler": HealRecipientPolicy(2050, 36, 61, 76),
	"phantomSummoner": HealRecipientPolicy(2350, 37, 64, 79),
	"shillienOracle": HealRecipientPolicy(3500, 42, 68, 82),
	"shillenElder": HealRecipientPolicy(3700, 45, 72, 86),
	"orcFighter": HealRecipientPolicy(2300, 34, 60, 74),
	"orcRaider": HealRecipientPolicy(2800, 38, 66, 80),
	"destroyer": HealRecipientPolicy(3200, 41, 70, 84),
	"orcMonk": HealRecipientPolicy(2300, 34, 60, 74),
	"tyrant": HealRecipientPolicy(2700, 37, 64, 78),
	"orcMage": HealRecipientPolicy(2900, 38, 65, 79),
	"orcShaman": HealRecipientPolicy(3350, 41, 67, 81),
	"overlord": HealRecipientPolicy(3400, 42, 68, 82),
	"warcryer": HealRecipientPolicy(3350, 41, 67, 81),
	"dwarvenFighter": HealRecipientPolicy(2500, 36, 62, 76),
	"scavenger": HealRecipientPolicy(2550, 36, 63, 77),
	"bountyHunter": HealRecipientPolicy(2600, 37, 64, 78),
	"artisan": HealRecipientPolicy(2550, 36, 63, 77),
	"warsmith": HealRecipientPolicy(2850, 39, 66, 80),
	"duelist": HealRecipientPolicy(2700, 37, 64, 78),
	"dreadnought": HealRecipientPolicy(3250, 42, 71, 85),
	"phoenixKnight": HealRecipientPolicy(4300, 46, 77, 91),
	"hellKnight": HealRecipientPolicy(4400, 47, 78, 92),
	"sagittarius": HealRecipientPolicy(1950, 31, 56, 71),
	"adventurer": HealRecipientPolicy(1600, 27, 50, 64),
	"archmage": HealRecipientPolicy(2050, 37, 62, 77),
	"soultaker": HealRecipientPolicy(2250, 38, 64, 79),
	"arcanaLord": HealRecipientPolicy(2400, 38, 65, 80),
	"cardinal": HealRecipientPolicy(3900, 48, 75, 89),
	"hierophant": HealRecipientPolicy(3450, 42, 69, 83),
	"evaTemplar": HealRecipientPolicy(4350, 46, 77, 91),
	"swordMuse": HealRecipientPolicy(3250, 41, 70, 84),
	"windRider": HealRecipientPolicy(1600, 26, 49, 63),
	"moonlightSentinel": HealRecipientPolicy(1950, 31, 56, 71),
	"mysticMuse": HealRecipientPolicy(2050, 37, 62, 77),
	"elementalMaster": HealRecipientPolicy(2400, 38, 65, 80),
	"evaSaint": HealRecipientPolicy(3900, 48, 75, 89),
	"shillienTemplar": HealRecipientPolicy(4400, 47, 78, 92),
	"spectralDancer": HealRecipientPolicy(3250, 41, 70, 84),
	"ghostHunter": HealRecipientPolicy(1600, 25, 48, 62),
	"ghostSentinel": HealRecipientPolicy(1950, 31, 56, 71),
	"stormScreamer": HealRecipientPolicy(2100, 37, 62, 77),
	"spectralMaster": HealRecipientPolicy(2400, 38, 65, 80),
	"shillienSaint": HealRecipientPolicy(3850, 47, 74, 88),
	"titan": HealRecipientPolicy(3300, 43, 72, 86),
	"grandKhauatari": HealRecipientPolicy(2750, 38, 65, 79),
	"dominator": HealRecipientPolicy(3500, 43, 70, 84),
	"doomcryer": HealRecipientPolicy(3450, 42, 69, 83),
	"fortuneSeeker": HealRecipientPolicy(2650, 38, 65, 79),
	"maestro": HealRecipientPolicy(2950, 40, 67, 81),
}

HEAL_PRIMARY_TANK_RECIPIENTS = (
	"knight",
	"paladin",
	"darkAvenger",
	"elvenKnight",
	"templeKnight",
	"palusKnight",
	"shillienKnight",
	"phoenixKnight",
	"hellKnight",
	"evaTemplar",
	"shillienTemplar",
)

HEAL_SKIRMISHER_RECIPIENTS = (
	"rogue",
	"treasureHunter",
	"elvenScout",
	"plainsWalker",
	"assassin",
	"abyssWalker",
	"adventurer",
	"windRider",
	"ghostHunter",
)


class ResurrectionRecipientPolicy(NamedTuple):
	"""One exact recipient's caster-authored recovery duty."""

	duty_priority: int


# Cleric keeps every recovery lane below emergency Battle Heal while preserving
# enough separation for current-context urgency inside each tactical band.
CLERIC_SAFE_RESURRECTION_BASE_PRIORITY = 16000
CLERIC_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 18000
CLERIC_CURE_BASE_PRIORITY = 25500

# Oracle's MP economy keeps recovery below emergency Battle Heal while leaving
# room for class-specific Recharge and current-context urgency.
ORACLE_SAFE_RESURRECTION_BASE_PRIORITY = 16000
ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 18000
ORACLE_CURE_BASE_PRIORITY = 25500

SHILLIEN_ORACLE_SAFE_RESURRECTION_BASE_PRIORITY = 16000
SHILLIEN_ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 18000
SHILLIEN_ORACLE_CURE_BASE_PRIORITY = 25500

# Song of Vengeance is maintained only when one of these exact shield-bearing
# front-line classes is present. It is not inferred from a tank role at runtime.
SONG_OF_VENGEANCE_RECIPIENTS = (
	"knight",
	"paladin",
	"darkAvenger",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"palusKnight",
	"shillienKnight",
	"phoenixKnight",
	"hellKnight",
	"evaTemplar",
	"swordMuse",
	"shillienTemplar",
)

# Song of Champion is reserved for these exact physical-skill progressions.
# Spell-oriented support classes that sometimes swing a weapon are not folded
# into this list by a generic fighter flag.
SONG_OF_CHAMPION_RECIPIENTS = (
	"fighter",
	"warrior",
	"gladiator",
	"warlord",
	"knight",
	"paladin",
	"darkAvenger",
	"rogue",
	"treasureHunter",
	"hawkeye",
	"elvenFighter",
	"elvenKnight",
	"templeKnight",
	"swordSinger",
	"elvenScout",
	"plainsWalker",
	"silverRanger",
	"darkFighter",
	"palusKnight",
	"shillienKnight",
	"bladedancer",
	"assassin",
	"abyssWalker",
	"phantomRanger",
	"orcFighter",
	"orcRaider",
	"destroyer",
	"orcMonk",
	"tyrant",
	"dwarvenFighter",
	"scavenger",
	"bountyHunter",
	"artisan",
	"warsmith",
	"duelist",
	"dreadnought",
	"phoenixKnight",
	"hellKnight",
	"sagittarius",
	"adventurer",
	"evaTemplar",
	"swordMuse",
	"windRider",
	"moonlightSentinel",
	"shillienTemplar",
	"spectralDancer",
	"ghostHunter",
	"ghostSentinel",
	"titan",
	"grandKhauatari",
	"fortuneSeeker",
	"maestro",
)


def buff(key, skill_id, priority, recipients, blocking_effects, conditions=(), relationship_priority=None):
	"""Declare one exact-caster buff with explicit exact recipients."""
	return {
		"key": key,
		"skill_id": skill_id,
		"priority": priority,
		"recipients": tuple(recipients),
		"blocking_effects": tuple(blocking_effects),
		"conditions": tuple(conditions),
		"relationship_priority": relationship_priority,
	}


def exact_support_buff(key, skill_id, priority, recipients, blocking_effects, mp_reserve=30):
	"""Declare one buff ordered by this exact caster's target relationship."""
	return buff(
		key,
		skill_id,
		priority,
		recipients,
		blocking_effects,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", mp_reserve)),
			condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
		),
		"ally_support_priority",
	)


def group_effect(key, skill_id, intent, priority, recipients, mp_reserve=40, radius=1000, blocking_effects=None, conditions=(), relationship_priority=None, relationship_priority_divisor=10):
	"""Declare one self-cast group effect gated by exact beneficiaries."""
	blocking_effects = (skill_id,) if blocking_effects is None else tuple(blocking_effects)
	return {
		"key": key,
		"skill_id": skill_id,
		"intent": intent,
		"priority": priority,
		"recipients": tuple(recipients),
		"mp_reserve": mp_reserve,
		"radius": radius,
		"blocking_effects": blocking_effects,
		"conditions": tuple(conditions),
		"relationship_priority": relationship_priority,
		"relationship_priority_divisor": relationship_priority_divisor,
	}


def exact_group_support_effect(key, skill_id, intent, priority, recipients, mp_reserve=40, radius=1000, blocking_effects=None):
	"""Declare one exact caster's party effect ordered by ally support duty."""
	return group_effect(key, skill_id, intent, priority, recipients, mp_reserve, radius, blocking_effects, relationship_priority="ally_support_priority")


def exact_group_recovery_effect(key, skill_id, priority, recipients, mp_reserve, conditions):
	"""Declare one exact caster's party recovery ordered by ally protection."""
	return group_effect(key, skill_id, "GROUP_HEAL", priority, recipients, mp_reserve, 1000, (skill_id,), conditions, "ally_protection_priority")


def condition(tag, *attributes):
	return {"tag": tag, "attributes": tuple(attributes)}


def targeted(key, skill_id, intent, target, priority, recipients, conditions):
	"""Declare one exact-caster action expanded for named exact recipients."""
	return {
		"key": key,
		"skill_id": skill_id,
		"intent": intent,
		"target": target,
		"priority": priority,
		"recipients": tuple(recipients),
		"conditions": tuple(conditions),
	}


def skill_policy(key, skill_id, intent, target, priority, conditions):
	"""Declare one non-matrix exact-caster policy."""
	return {
		"key": key,
		"skill_id": skill_id,
		"intent": intent,
		"target": target,
		"priority": priority,
		"conditions": tuple(conditions),
	}


def recharge(key, priority, threshold, recipients, self_reserve=45):
	return targeted(
		key,
		1013,
		"RECHARGE",
		"PARTY_LOWEST_MP",
		priority,
		recipients,
		(
			condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "TARGET"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", threshold)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", self_reserve)),
			condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
		),
	)


EXACT_RECHARGE_MATRICES = (
	recharge("recharge-priority", 12500, 60, RECHARGE_PRIORITY_RECIPIENTS),
	recharge("recharge-skill", 12000, 35, RECHARGE_SKILL_RECIPIENTS),
	recharge("recharge-reserve", 11500, 20, RECHARGE_RESERVE_RECIPIENTS),
)


def exact_recharge_matrices(source_class, self_reserve=45):
	"""Materialize caster-owned Recharge priority for every eligible exact class."""
	matrices = []
	for key, base_priority, threshold, recipients in (
		("recharge-priority", 12500, 60, RECHARGE_PRIORITY_RECIPIENTS),
		("recharge-skill", 12000, 35, RECHARGE_SKILL_RECIPIENTS),
		("recharge-reserve", 11500, 20, RECHARGE_RESERVE_RECIPIENTS),
	):
		for recipient in recipients:
			support_priority = EXACT_CLASS_RELATIONSHIP_MATRICES[source_class][recipient].ally_support_priority
			matrices.append(recharge(key, base_priority + (support_priority // 10), threshold, (recipient,), self_reserve))
	return tuple(matrices)


EXACT_ORACLE_RECHARGE_MATRICES = exact_recharge_matrices("oracle")
EXACT_SHILLIEN_ORACLE_RECHARGE_MATRICES = exact_recharge_matrices("shillienOracle")
EXACT_ELDER_RECHARGE_MATRICES = exact_recharge_matrices("elder")
EXACT_EVA_SAINT_RECHARGE_MATRICES = exact_recharge_matrices("evaSaint")


def exact_heal_matrices(duty_policies=HEAL_RECIPIENT_POLICIES):
	matrices = []
	for recipient, policy in HEAL_RECIPIENT_POLICIES.items():
		duty_priority = duty_policies[recipient].duty_priority
		common = (
			condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True)),
			condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
		)
		matrices.extend((
			targeted(
				"battle-heal",
				1015,
				"HEAL",
				"PARTY_LOWEST_HP",
				30000 + duty_priority,
				(recipient,),
				(
					common[0],
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.battle_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 5)),
					common[1],
				),
			),
			targeted(
				"major-heal",
				1401,
				"HEAL",
				"PARTY_LOWEST_HP",
				20000 + duty_priority,
				(recipient,),
				(
					common[0],
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.major_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 15)),
					common[1],
				),
			),
			targeted(
				"greater-heal",
				1217,
				"HEAL",
				"PARTY_LOWEST_HP",
				10000 + duty_priority,
				(recipient,),
				(
					common[0],
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.greater_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 25)),
					common[1],
				),
			),
		))
	return tuple(matrices)


EXACT_HEAL_MATRICES = exact_heal_matrices()


def exact_two_lane_heal_matrices(efficient_key, efficient_skill_id, duty_policies=HEAL_RECIPIENT_POLICIES):
	matrices = []
	for recipient, policy in HEAL_RECIPIENT_POLICIES.items():
		duty_priority = duty_policies[recipient].duty_priority
		common = (
			condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True)),
			condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
		)
		matrices.extend((
			targeted(
				"battle-heal",
				1015,
				"HEAL",
				"PARTY_LOWEST_HP",
				30000 + duty_priority,
				(recipient,),
				(
					common[0],
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.battle_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 5)),
					common[1],
				),
			),
			targeted(
				efficient_key,
				efficient_skill_id,
				"HEAL",
				"PARTY_LOWEST_HP",
				10000 + duty_priority,
				(recipient,),
				(
					common[0],
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.greater_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 25)),
					common[1],
				),
			),
		))
	return tuple(matrices)


EXACT_SHILLIEN_HEAL_MATRICES = exact_two_lane_heal_matrices("greater-heal", 1217)
EXACT_NOVICE_HEAL_MATRICES = exact_two_lane_heal_matrices("heal", 1011)

PROPHET_SAFE_RESURRECTION_BASE_PRIORITY = 17000
PROPHET_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 19000
PROPHET_CURE_BASE_PRIORITY = 26000


def exact_protection_recipient_policies(source_class):
	"""Materialize one exact caster's ally-protection duty for all classes."""
	return {
		recipient: ResurrectionRecipientPolicy(relationship.ally_protection_priority)
		for recipient, relationship in EXACT_CLASS_RELATIONSHIP_MATRICES[source_class].items()
	}


CLERIC_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("cleric")
EXACT_CLERIC_HEAL_MATRICES = exact_two_lane_heal_matrices("heal", 1011, CLERIC_RECOVERY_RECIPIENT_POLICIES)

ORACLE_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("oracle")
EXACT_ORACLE_HEAL_MATRICES = exact_two_lane_heal_matrices("heal", 1011, ORACLE_RECOVERY_RECIPIENT_POLICIES)

SHILLIEN_ORACLE_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("shillienOracle")
EXACT_SHILLIEN_ORACLE_HEAL_MATRICES = exact_two_lane_heal_matrices("heal", 1011, SHILLIEN_ORACLE_RECOVERY_RECIPIENT_POLICIES)

PROPHET_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("prophet")
EXACT_PROPHET_HEAL_MATRICES = exact_two_lane_heal_matrices("heal", 1011, PROPHET_RECOVERY_RECIPIENT_POLICIES)

HIEROPHANT_SAFE_RESURRECTION_BASE_PRIORITY = 17000
HIEROPHANT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 19000
HIEROPHANT_CURE_BASE_PRIORITY = 25500
HIEROPHANT_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("hierophant")
EXACT_HIEROPHANT_HEAL_MATRICES = exact_two_lane_heal_matrices("heal", 1011, HIEROPHANT_RECOVERY_RECIPIENT_POLICIES)

ELDER_SAFE_RESURRECTION_BASE_PRIORITY = 17000
ELDER_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 19000
ELDER_CURE_POISON_BASE_PRIORITY = 25100
ELDER_VITALIZE_BLEED_BASE_PRIORITY = 25700
ELDER_VITALIZE_HEAL_BASE_PRIORITY = 28500
ELDER_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("elder")
EXACT_ELDER_HEAL_MATRICES = exact_heal_matrices(ELDER_RECOVERY_RECIPIENT_POLICIES)

EVA_SAINT_SAFE_RESURRECTION_BASE_PRIORITY = 17000
EVA_SAINT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 19000
EVA_SAINT_CURE_POISON_BASE_PRIORITY = 25000
EVA_SAINT_VITALIZE_BLEED_BASE_PRIORITY = 25700
EVA_SAINT_VITALIZE_HEAL_BASE_PRIORITY = 28500
EVA_SAINT_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("evaSaint")
EXACT_EVA_SAINT_HEAL_MATRICES = exact_heal_matrices(EVA_SAINT_RECOVERY_RECIPIENT_POLICIES)

BISHOP_SAFE_RESURRECTION_BASE_PRIORITY = 17000
BISHOP_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 19000
BISHOP_CURE_POISON_BASE_PRIORITY = 25000
BISHOP_PURIFY_BLEED_BASE_PRIORITY = 25300
BISHOP_PURIFY_PARALYSIS_BASE_PRIORITY = 25900
BISHOP_RESTORE_LIFE_BASE_PRIORITY = 27000
BISHOP_VITALIZE_BASE_PRIORITY = 28500
BISHOP_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("bishop")

CARDINAL_SAFE_RESURRECTION_BASE_PRIORITY = 17000
CARDINAL_BATTLEFIELD_RESURRECTION_BASE_PRIORITY = 19000
CARDINAL_CURE_POISON_BASE_PRIORITY = 25000
CARDINAL_PURIFY_BLEED_BASE_PRIORITY = 25300
CARDINAL_PURIFY_PARALYSIS_BASE_PRIORITY = 25900
CARDINAL_RESTORE_LIFE_BASE_PRIORITY = 27000
CARDINAL_VITALIZE_BASE_PRIORITY = 28500
CARDINAL_RECOVERY_RECIPIENT_POLICIES = exact_protection_recipient_policies("cardinal")


def exact_status_cure_matrices(key, skill_id, status, base_priority, duty_policies, mp_reserve):
	"""Emit one ailment-gated cure for every exact party recipient."""
	return tuple(
		targeted(
			key,
			skill_id,
			"CURE",
			"PARTY_MEMBER",
			base_priority + duty_policies[recipient].duty_priority,
			(recipient,),
			(
				condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True)),
				condition("status", ("subject", "TARGET"), ("status", status), ("expected", True)),
				condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", mp_reserve)),
				condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
			),
		)
		for recipient in ALL_PLAYABLE_CLASSES
	)


EXACT_CLERIC_CURE_MATRICES = exact_status_cure_matrices("cure-poison", 1012, "POISONED", CLERIC_CURE_BASE_PRIORITY, CLERIC_RECOVERY_RECIPIENT_POLICIES, 15)
EXACT_ORACLE_CURE_MATRICES = exact_status_cure_matrices("cure-poison", 1012, "POISONED", ORACLE_CURE_BASE_PRIORITY, ORACLE_RECOVERY_RECIPIENT_POLICIES, 20)
EXACT_SHILLIEN_ORACLE_CURE_MATRICES = exact_status_cure_matrices("cure-poison", 1012, "POISONED", SHILLIEN_ORACLE_CURE_BASE_PRIORITY, SHILLIEN_ORACLE_RECOVERY_RECIPIENT_POLICIES, 20)
EXACT_PROPHET_CURE_MATRICES = exact_status_cure_matrices("cure-poison", 1012, "POISONED", PROPHET_CURE_BASE_PRIORITY, PROPHET_RECOVERY_RECIPIENT_POLICIES, 20)
EXACT_HIEROPHANT_CURE_MATRICES = exact_status_cure_matrices("cure-poison", 1012, "POISONED", HIEROPHANT_CURE_BASE_PRIORITY, HIEROPHANT_RECOVERY_RECIPIENT_POLICIES, 20)
EXACT_ELDER_CURE_MATRICES = (
	*exact_status_cure_matrices("cure-poison", 1012, "POISONED", ELDER_CURE_POISON_BASE_PRIORITY, ELDER_RECOVERY_RECIPIENT_POLICIES, 25),
	*exact_status_cure_matrices("vitalize-bleeding", 1020, "BLEEDING", ELDER_VITALIZE_BLEED_BASE_PRIORITY, ELDER_RECOVERY_RECIPIENT_POLICIES, 30),
)
EXACT_EVA_SAINT_CURE_MATRICES = (
	*exact_status_cure_matrices("cure-poison", 1012, "POISONED", EVA_SAINT_CURE_POISON_BASE_PRIORITY, EVA_SAINT_RECOVERY_RECIPIENT_POLICIES, 25),
	*exact_status_cure_matrices("vitalize-bleeding", 1020, "BLEEDING", EVA_SAINT_VITALIZE_BLEED_BASE_PRIORITY, EVA_SAINT_RECOVERY_RECIPIENT_POLICIES, 30),
)
EXACT_BISHOP_CURE_MATRICES = (
	*exact_status_cure_matrices("cure-poison", 1012, "POISONED", BISHOP_CURE_POISON_BASE_PRIORITY, BISHOP_RECOVERY_RECIPIENT_POLICIES, 20),
	*exact_status_cure_matrices("purify-bleeding", 1018, "BLEEDING", BISHOP_PURIFY_BLEED_BASE_PRIORITY, BISHOP_RECOVERY_RECIPIENT_POLICIES, 25),
	*exact_status_cure_matrices("purify-paralysis", 1018, "PARALYZED", BISHOP_PURIFY_PARALYSIS_BASE_PRIORITY, BISHOP_RECOVERY_RECIPIENT_POLICIES, 25),
)
EXACT_CARDINAL_INHERITED_CURE_MATRICES = (
	*exact_status_cure_matrices("cure-poison", 1012, "POISONED", CARDINAL_CURE_POISON_BASE_PRIORITY, CARDINAL_RECOVERY_RECIPIENT_POLICIES, 20),
	*exact_status_cure_matrices("purify-bleeding", 1018, "BLEEDING", CARDINAL_PURIFY_BLEED_BASE_PRIORITY, CARDINAL_RECOVERY_RECIPIENT_POLICIES, 25),
	*exact_status_cure_matrices("purify-paralysis", 1018, "PARALYZED", CARDINAL_PURIFY_PARALYSIS_BASE_PRIORITY, CARDINAL_RECOVERY_RECIPIENT_POLICIES, 25),
)


def exact_resurrection_matrices(recipient_policies, safe_base_priority, battlefield_base_priority, safe_mp_reserve, battlefield_mp_reserve):
	"""Emit policies from one caster's complete exact-recipient decisions."""
	matrices = []
	for recipient, policy in recipient_policies.items():
		common = (
			condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", False)),
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 900)),
		)
		matrices.append(targeted(
			"battlefield-resurrection",
			1016,
			"RESURRECT",
			"PARTY_DEAD_HIGHEST_LEVEL",
			battlefield_base_priority + policy.duty_priority,
			(recipient,),
			(
				common[0],
				common[1],
				condition("status", ("subject", "SELF"), ("status", "IN_COMBAT"), ("expected", True)),
				condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", battlefield_mp_reserve)),
				common[2],
			),
		))
		matrices.append(targeted(
			"safe-resurrection",
			1016,
			"RESURRECT",
			"PARTY_DEAD_HIGHEST_LEVEL",
			safe_base_priority + policy.duty_priority,
			(recipient,),
			(
				common[0],
				common[1],
				condition("status", ("subject", "SELF"), ("status", "IN_COMBAT"), ("expected", False)),
				condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", safe_mp_reserve)),
				common[2],
			),
		))
	return tuple(matrices)


EXACT_CLERIC_RESURRECTION_MATRICES = exact_resurrection_matrices(CLERIC_RECOVERY_RECIPIENT_POLICIES, CLERIC_SAFE_RESURRECTION_BASE_PRIORITY, CLERIC_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 25, 35)
EXACT_ORACLE_RESURRECTION_MATRICES = exact_resurrection_matrices(ORACLE_RECOVERY_RECIPIENT_POLICIES, ORACLE_SAFE_RESURRECTION_BASE_PRIORITY, ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 35, 45)
EXACT_SHILLIEN_ORACLE_RESURRECTION_MATRICES = exact_resurrection_matrices(SHILLIEN_ORACLE_RECOVERY_RECIPIENT_POLICIES, SHILLIEN_ORACLE_SAFE_RESURRECTION_BASE_PRIORITY, SHILLIEN_ORACLE_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 35, 45)
EXACT_PROPHET_RESURRECTION_MATRICES = exact_resurrection_matrices(PROPHET_RECOVERY_RECIPIENT_POLICIES, PROPHET_SAFE_RESURRECTION_BASE_PRIORITY, PROPHET_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 40, 50)
EXACT_HIEROPHANT_RESURRECTION_MATRICES = exact_resurrection_matrices(HIEROPHANT_RECOVERY_RECIPIENT_POLICIES, HIEROPHANT_SAFE_RESURRECTION_BASE_PRIORITY, HIEROPHANT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 40, 50)
EXACT_ELDER_RESURRECTION_MATRICES = exact_resurrection_matrices(ELDER_RECOVERY_RECIPIENT_POLICIES, ELDER_SAFE_RESURRECTION_BASE_PRIORITY, ELDER_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 45, 55)
EXACT_EVA_SAINT_RESURRECTION_MATRICES = exact_resurrection_matrices(EVA_SAINT_RECOVERY_RECIPIENT_POLICIES, EVA_SAINT_SAFE_RESURRECTION_BASE_PRIORITY, EVA_SAINT_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 45, 55)
EXACT_BISHOP_RESURRECTION_MATRICES = exact_resurrection_matrices(BISHOP_RECOVERY_RECIPIENT_POLICIES, BISHOP_SAFE_RESURRECTION_BASE_PRIORITY, BISHOP_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 35, 45)
EXACT_CARDINAL_RESURRECTION_MATRICES = exact_resurrection_matrices(CARDINAL_RECOVERY_RECIPIENT_POLICIES, CARDINAL_SAFE_RESURRECTION_BASE_PRIORITY, CARDINAL_BATTLEFIELD_RESURRECTION_BASE_PRIORITY, 35, 45)


def exact_vitalize_heal_matrices(duty_policies, base_priority):
	"""Use Vitalize when one cast can heal and remove poison or bleed."""
	matrices = []
	for recipient, thresholds in HEAL_RECIPIENT_POLICIES.items():
		duty_priority = duty_policies[recipient].duty_priority
		for key, status in (("vitalize-poison-recovery", "POISONED"), ("vitalize-bleeding-recovery", "BLEEDING")):
			matrices.append(targeted(
				key,
				1020,
				"HEAL",
				"PARTY_LOWEST_HP",
				base_priority + duty_priority,
				(recipient,),
				(
					condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True)),
					condition("status", ("subject", "TARGET"), ("status", status), ("expected", True)),
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", thresholds.major_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 30)),
					condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
				),
			))
	return tuple(matrices)


EXACT_ELDER_VITALIZE_HEAL_MATRICES = exact_vitalize_heal_matrices(ELDER_RECOVERY_RECIPIENT_POLICIES, ELDER_VITALIZE_HEAL_BASE_PRIORITY)
EXACT_EVA_SAINT_VITALIZE_HEAL_MATRICES = exact_vitalize_heal_matrices(EVA_SAINT_RECOVERY_RECIPIENT_POLICIES, EVA_SAINT_VITALIZE_HEAL_BASE_PRIORITY)


def exact_bishop_lineage_heal_matrices(duty_policies):
	"""Emit the inherited Bishop-lineage protection and core heal decisions."""
	matrices = []
	for recipient, policy in HEAL_RECIPIENT_POLICIES.items():
		duty_priority = duty_policies[recipient].duty_priority
		alive = condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True))
		range_condition = condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200))
		matrices.extend((
			targeted(
				"celestial-shield",
				1418,
				"BUFF",
				"PARTY_LOWEST_HP",
				40000 + duty_priority,
				(recipient,),
				(
					alive,
					condition("status", ("subject", "TARGET"), ("status", "IN_COMBAT"), ("expected", True)),
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.battle_threshold + 10)),
					condition("effect", ("subject", "TARGET"), ("skillId", 1418), ("state", "ABSENT")),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 10)),
					range_condition,
				),
			),
			targeted(
				"greater-battle-heal",
				1218,
				"HEAL",
				"PARTY_LOWEST_HP",
				30000 + duty_priority,
				(recipient,),
				(
					alive,
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.battle_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 5)),
					range_condition,
				),
			),
			targeted(
				"major-heal",
				1401,
				"HEAL",
				"PARTY_LOWEST_HP",
				20000 + duty_priority,
				(recipient,),
				(
					alive,
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.major_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 15)),
					range_condition,
				),
			),
			targeted(
				"greater-heal",
				1217,
				"HEAL",
				"PARTY_LOWEST_HP",
				10000 + duty_priority,
				(recipient,),
				(
					alive,
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.greater_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 25)),
					range_condition,
				),
			),
		))
	return tuple(matrices)


def exact_bishop_lineage_specialist_heal_matrices(duty_policies, restore_life_base_priority, vitalize_base_priority):
	"""Emit inherited percentage-heal and combined heal/cure decisions."""
	matrices = []
	for recipient, policy in HEAL_RECIPIENT_POLICIES.items():
		duty_priority = duty_policies[recipient].duty_priority
		alive = condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True))
		range_condition = condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200))
		matrices.append(targeted(
			"restore-life",
			1258,
			"HEAL",
			"PARTY_LOWEST_HP",
			restore_life_base_priority + duty_priority,
			(recipient,),
			(
				alive,
				condition("status", ("subject", "SELF"), ("status", "IN_COMBAT"), ("expected", False)),
				condition("status", ("subject", "TARGET"), ("status", "IN_COMBAT"), ("expected", False)),
				condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.major_threshold)),
				condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 35)),
				range_condition,
			),
		))
		for key, status in (("vitalize-poison", "POISONED"), ("vitalize-bleeding", "BLEEDING")):
			matrices.append(targeted(
				key,
				1020,
				"HEAL",
				"PARTY_LOWEST_HP",
				vitalize_base_priority + duty_priority,
				(recipient,),
				(
					alive,
					condition("status", ("subject", "TARGET"), ("status", status), ("expected", True)),
					condition("resource", ("subject", "TARGET"), ("resource", "HP"), ("amount", "PERCENT"), ("comparison", "BELOW"), ("value", policy.major_threshold)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 30)),
					range_condition,
				),
			))
	return tuple(matrices)


EXACT_BISHOP_HEAL_MATRICES = exact_bishop_lineage_heal_matrices(BISHOP_RECOVERY_RECIPIENT_POLICIES) + exact_bishop_lineage_specialist_heal_matrices(BISHOP_RECOVERY_RECIPIENT_POLICIES, BISHOP_RESTORE_LIFE_BASE_PRIORITY, BISHOP_VITALIZE_BASE_PRIORITY)
EXACT_CARDINAL_HEAL_MATRICES = exact_bishop_lineage_heal_matrices(CARDINAL_RECOVERY_RECIPIENT_POLICIES) + exact_bishop_lineage_specialist_heal_matrices(CARDINAL_RECOVERY_RECIPIENT_POLICIES, CARDINAL_RESTORE_LIFE_BASE_PRIORITY, CARDINAL_VITALIZE_BASE_PRIORITY)

BISHOP_GROUP_HEAL_POLICIES = (
	skill_policy(
		"benediction",
		1271,
		"GROUP_HEAL",
		"SELF",
		39000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "AT_MOST"), ("value", 25)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 4), ("threshold", 35), ("radius", 1000)),
		),
	),
	skill_policy(
		"major-group-heal",
		1402,
		"GROUP_HEAL",
		"SELF",
		29000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 30)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 4), ("threshold", 65), ("radius", 1000)),
		),
	),
	skill_policy(
		"greater-group-heal",
		1219,
		"GROUP_HEAL",
		"SELF",
		28000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 25)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 3), ("threshold", 55), ("radius", 1000)),
		),
	),
	skill_policy(
		"group-heal",
		1027,
		"GROUP_HEAL",
		"SELF",
		27000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 20)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 3), ("threshold", 45), ("radius", 1000)),
		),
	),
)

BISHOP_PARTY_WIDE_POLICIES = BISHOP_GROUP_HEAL_POLICIES + (
	skill_policy(
		"body-of-avatar-emergency",
		1311,
		"GROUP_HEAL",
		"SELF",
		39000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("status", ("subject", "SELF"), ("status", "IN_COMBAT"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 35)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 3), ("threshold", 55), ("radius", 1000)),
		),
	),
	skill_policy(
		"prayer",
		1307,
		"GROUP_BUFF",
		"SELF",
		9900,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 55)),
			condition("effectCount", ("group", "PARTY"), ("effectSkillIds", "1307"), ("effectState", "ABSENT"), ("comparison", "AT_LEAST"), ("value", 1), ("radius", 1000)),
		),
	),
	skill_policy(
		"body-of-avatar-maintenance",
		1311,
		"GROUP_BUFF",
		"SELF",
		9800,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("status", ("subject", "SELF"), ("status", "IN_COMBAT"), ("expected", False)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 65)),
			condition("count", ("group", "PARTY"), ("filter", "ALIVE"), ("comparison", "AT_LEAST"), ("value", 2), ("radius", 1000)),
			condition("effectCount", ("group", "PARTY"), ("effectSkillIds", "1045,1311"), ("effectState", "ABSENT"), ("comparison", "AT_LEAST"), ("value", 1), ("radius", 1000)),
		),
	),
)

NOVICE_GROUP_HEAL_POLICIES = (
	skill_policy(
		"group-heal",
		1027,
		"GROUP_HEAL",
		"SELF",
		27000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 20)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 3), ("threshold", 45), ("radius", 1000)),
		),
	),
)


CARDINAL_CLEANSE_STATUS_POLICIES = (
	("generic-debuff", "DEBUFFED", 25050),
	("weakness", "WEAKENED", 25100),
	("confusion", "CONFUSED", 25400),
	("root", "ROOTED", 25500),
	("silence", "SILENCED", 25600),
	("physical-mute", "MUTED", 25600),
	("sleep", "SLEEPING", 25700),
	("fear", "FEARED", 25800),
	("stun", "STUNNED", 25900),
)


def exact_cardinal_cleanse_matrices():
	"""Spend Holy Water only on ailments without a cheaper inherited cure."""
	matrices = []
	for recipient in ALL_PLAYABLE_CLASSES:
		duty_priority = CARDINAL_RECOVERY_RECIPIENT_POLICIES[recipient].duty_priority
		for key, status, base_priority in CARDINAL_CLEANSE_STATUS_POLICIES:
			matrices.append(targeted(
				f"cleanse-{key}",
				1409,
				"CURE",
				"PARTY_MEMBER",
				base_priority + duty_priority,
				(recipient,),
				(
					condition("status", ("subject", "TARGET"), ("status", "ALIVE"), ("expected", True)),
					condition("status", ("subject", "TARGET"), ("status", status), ("expected", True)),
					condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 25)),
					condition("distance", ("subject", "TARGET"), ("comparison", "AT_MOST"), ("value", 1200)),
				),
			))
	return tuple(matrices)


EXACT_CARDINAL_CLEANSE_MATRICES = exact_cardinal_cleanse_matrices()

CARDINAL_PARTY_WIDE_POLICIES = BISHOP_PARTY_WIDE_POLICIES + (
	skill_policy(
		"balance-life",
		1335,
		"GROUP_HEAL",
		"SELF",
		38000,
		(
			condition("status", ("subject", "SELF"), ("status", "ALIVE"), ("expected", True)),
			condition("resource", ("subject", "SELF"), ("resource", "MP"), ("amount", "PERCENT"), ("comparison", "ABOVE"), ("value", 20)),
			condition("count", ("group", "PARTY"), ("filter", "ALIVE"), ("comparison", "AT_LEAST"), ("value", 3), ("radius", 1000)),
			condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 1), ("threshold", 35), ("radius", 1000)),
		),
	),
)

SWORD_SINGER_GROUP_EFFECTS = (
	group_effect("song-of-earth", 264, "SONG", 10000, ALL_PLAYABLE_CLASSES),
	group_effect("song-of-warding", 267, "SONG", 9900, ALL_PLAYABLE_CLASSES),
	group_effect("song-of-vitality", 304, "SONG", 9800, ALL_PLAYABLE_CLASSES),
	group_effect("song-of-hunter", 269, "SONG", 9700, MIGHT_RECIPIENTS),
	group_effect("song-of-wind", 268, "SONG", 9600, ALL_PLAYABLE_CLASSES),
	group_effect("song-of-water", 266, "SONG", 9500, AGILITY_RECIPIENTS),
	group_effect("song-of-vengeance", 305, "SONG", 9400, SONG_OF_VENGEANCE_RECIPIENTS),
	group_effect("song-of-life", 265, "SONG", 9000, ALL_PLAYABLE_CLASSES),
)

BLADEDANCER_GROUP_EFFECTS = (
	group_effect("dance-of-warrior", 271, "DANCE", 10000, SONG_OF_CHAMPION_RECIPIENTS),
	group_effect("dance-of-mystic", 273, "DANCE", 10000, EMPOWER_RECIPIENTS),
	group_effect("dance-of-fire", 274, "DANCE", 9900, SONG_OF_CHAMPION_RECIPIENTS),
	group_effect("dance-of-concentration", 276, "DANCE", 9900, CASTING_RECIPIENTS),
	group_effect("dance-of-fury", 275, "DANCE", 9800, SONG_OF_CHAMPION_RECIPIENTS),
	group_effect("dance-of-inspiration", 272, "DANCE", 9700, SONG_OF_CHAMPION_RECIPIENTS),
	group_effect("dance-of-vampire", 310, "DANCE", 9600, VAMPIRIC_RAGE_RECIPIENTS),
)

SPECTRAL_DANCER_GROUP_EFFECTS = BLADEDANCER_GROUP_EFFECTS + (
	group_effect("sirens-dance", 365, "DANCE", 11000, WILD_MAGIC_RECIPIENTS),
)

def warcryer_group_effects(effect):
	"""Author the inherited chant inventory with an exact-class emitter."""
	return (
		effect("war-chant", 1390, "CHANT", 10000, GREATER_MIGHT_RECIPIENTS, mp_reserve=30, blocking_effects=(1388, 1389, 1390, 1391)),
		effect("earth-chant", 1391, "CHANT", 10000, GREATER_SHIELD_RECIPIENTS, mp_reserve=30, blocking_effects=(1388, 1389, 1390, 1391)),
		effect("chant-of-fire", 1006, "CHANT", 9800, ALL_PLAYABLE_CLASSES, mp_reserve=30, blocking_effects=(72, 123, 1006, 1008, 1036)),
		effect("chant-of-shielding", 1009, "CHANT", 9700, ALL_PLAYABLE_CLASSES, mp_reserve=30, blocking_effects=(1005, 1009, 1010, 1040)),
		effect("chant-of-battle", 1007, "CHANT", 9600, SONG_OF_CHAMPION_RECIPIENTS, mp_reserve=30, blocking_effects=(1003, 1007, 1068)),
		effect("flame-chant", 1002, "CHANT", 9600, CASTING_RECIPIENTS, mp_reserve=30, blocking_effects=(1002, 1004, 1085)),
		effect("chant-of-fury", 1251, "CHANT", 9500, SONG_OF_CHAMPION_RECIPIENTS, mp_reserve=30, blocking_effects=(1086, 1251)),
		effect("chant-of-evasion", 1252, "CHANT", 9400, AGILITY_RECIPIENTS, mp_reserve=30, blocking_effects=(1087, 1252)),
		effect("chant-of-rage", 1253, "CHANT", 9300, SONG_OF_CHAMPION_RECIPIENTS, mp_reserve=30, blocking_effects=(1242, 1253, 1364)),
		effect("chant-of-revenge", 1284, "CHANT", 9200, SONG_OF_VENGEANCE_RECIPIENTS, mp_reserve=30, blocking_effects=(305, 1284)),
		effect("chant-of-predator", 1308, "CHANT", 9100, SONG_OF_CHAMPION_RECIPIENTS, mp_reserve=30, blocking_effects=(1077, 1308)),
		effect("chant-of-eagle", 1309, "CHANT", 9000, SONG_OF_CHAMPION_RECIPIENTS, mp_reserve=30, blocking_effects=(1240, 1249, 1309)),
		effect("chant-of-vampire", 1310, "CHANT", 8900, VAMPIRIC_RAGE_RECIPIENTS, mp_reserve=30, blocking_effects=(1268, 1310)),
	)


EXACT_WARCRYER_GROUP_EFFECTS = warcryer_group_effects(exact_group_support_effect)


def warcryer_group_recovery_effects():
	"""Materialize one class-owned Chant of Life recovery contract."""
	return (
		exact_group_recovery_effect(
			"chant-of-life",
			1229,
			27000,
			ALL_PLAYABLE_CLASSES,
			45,
			(
				condition("status", ("subject", "SELF"), ("status", "IN_COMBAT"), ("expected", True)),
				condition("count", ("group", "PARTY"), ("filter", "HP_PERCENT_BELOW"), ("comparison", "AT_LEAST"), ("value", 2), ("threshold", 75), ("radius", 1000)),
			),
		),
	)


WARCRYER_GROUP_RECOVERY_EFFECTS = warcryer_group_recovery_effects()
EXACT_DOOMCRYER_GROUP_EFFECTS = warcryer_group_effects(exact_group_support_effect) + (
	exact_group_support_effect("chant-of-victory", 1363, "CHANT", 12000, CHANT_OF_VICTORY_RECIPIENTS, mp_reserve=45, blocking_effects=(1355, 1356, 1357, 1363, 1413, 1414)),
	exact_group_support_effect("magnus-chant", 1413, "CHANT", 12000, MAGNUS_CHANT_RECIPIENTS, mp_reserve=45, blocking_effects=(1355, 1356, 1357, 1363, 1413, 1414)),
	exact_group_support_effect("chant-of-spirit", 1362, "CHANT", 11000, ALL_PLAYABLE_CLASSES, mp_reserve=40, radius=900),
)
DOOMCRYER_GROUP_RECOVERY_EFFECTS = warcryer_group_recovery_effects()


CATALOG = {
	"hierophant": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["hierophant"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 100,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_HIEROPHANT_HEAL_MATRICES + EXACT_HIEROPHANT_CURE_MATRICES + EXACT_HIEROPHANT_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("prophecy-of-fire", 1356, 10000, PROPHECY_OF_FIRE_RECIPIENTS, (1355, 1356, 1357, 1363, 1413, 1414), mp_reserve=45),
			exact_support_buff("greater-might", 1388, 9800, GREATER_MIGHT_RECIPIENTS, (1388, 1389, 1390, 1391)),
			exact_support_buff("greater-shield", 1389, 9800, GREATER_SHIELD_RECIPIENTS, (1388, 1389, 1390, 1391)),
			exact_support_buff("bless-body", 1045, 9600, ALL_PLAYABLE_CLASSES, (1045, 1311)),
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("magic-barrier", 1036, 9400, ALL_PLAYABLE_CLASSES, (72, 123, 1006, 1008, 1036)),
			exact_support_buff("mental-shield", 1035, 9300, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("elemental-protection", 1352, 9200, ALL_PLAYABLE_CLASSES, (1352,), mp_reserve=35),
			exact_support_buff("wind-walk", 1204, 9100, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 8900, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("haste", 1086, 8800, MIGHT_RECIPIENTS, (1086, 1251)),
			exact_support_buff("focus", 1077, 8700, MIGHT_RECIPIENTS, (1077, 1308)),
			exact_support_buff("death-whisper", 1242, 8600, MIGHT_RECIPIENTS, (1242, 1253, 1364)),
			exact_support_buff("guidance", 1240, 8500, MIGHT_RECIPIENTS, (1240, 1249, 1309)),
			exact_support_buff("acumen", 1085, 8900, CASTING_RECIPIENTS, (1002, 1004, 1085)),
			exact_support_buff("concentration", 1078, 8800, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("bless-soul", 1048, 8700, CASTING_RECIPIENTS, (1048, 303)),
			exact_support_buff("berserker-spirit", 1062, 8400, BERSERKER_SPIRIT_RECIPIENTS, (1062,)),
			exact_support_buff("bless-shield", 1243, 8300, SHIELD_BLOCK_RECIPIENTS, (1243, 1250)),
			exact_support_buff("regeneration", 1044, 8200, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			exact_support_buff("invigor", 1032, 8100, ALL_PLAYABLE_CLASSES, (1032,)),
			exact_support_buff("resist-poison", 1033, 8000, ALL_PLAYABLE_CLASSES, (1033,)),
			# Standalone elemental, holy/unholy, weapon-attribute, and breath
			# buffs remain encounter- or environment-gated rather than always on.
		),
	},
	"prophet": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["prophet"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 100,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_PROPHET_HEAL_MATRICES + EXACT_PROPHET_CURE_MATRICES + EXACT_PROPHET_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("greater-might", 1388, 9800, GREATER_MIGHT_RECIPIENTS, (1388, 1389, 1390, 1391)),
			exact_support_buff("greater-shield", 1389, 9800, GREATER_SHIELD_RECIPIENTS, (1388, 1389, 1390, 1391)),
			exact_support_buff("bless-body", 1045, 9600, ALL_PLAYABLE_CLASSES, (1045, 1311)),
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("magic-barrier", 1036, 9400, ALL_PLAYABLE_CLASSES, (72, 123, 1006, 1008, 1036)),
			exact_support_buff("mental-shield", 1035, 9300, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9100, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 8900, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("haste", 1086, 8800, MIGHT_RECIPIENTS, (1086, 1251)),
			exact_support_buff("focus", 1077, 8700, MIGHT_RECIPIENTS, (1077, 1308)),
			exact_support_buff("death-whisper", 1242, 8600, MIGHT_RECIPIENTS, (1242, 1253, 1364)),
			exact_support_buff("guidance", 1240, 8500, MIGHT_RECIPIENTS, (1240, 1249, 1309)),
			exact_support_buff("acumen", 1085, 8900, CASTING_RECIPIENTS, (1002, 1004, 1085)),
			exact_support_buff("concentration", 1078, 8800, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("bless-soul", 1048, 8700, CASTING_RECIPIENTS, (1048, 303)),
			exact_support_buff("berserker-spirit", 1062, 8400, BERSERKER_SPIRIT_RECIPIENTS, (1062,)),
			exact_support_buff("bless-shield", 1243, 8300, SHIELD_BLOCK_RECIPIENTS, (1243, 1250)),
			exact_support_buff("regeneration", 1044, 8200, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			exact_support_buff("invigor", 1032, 8100, ALL_PLAYABLE_CLASSES, (1032,)),
			exact_support_buff("resist-poison", 1033, 8000, ALL_PLAYABLE_CLASSES, (1033,)),
			# Elemental, holy/unholy, weapon-attribute, and breath buffs require
			# encounter or environment evidence; never consume finite buff slots
			# merely because the Prophet knows the skill.
		),
	},
	"cleric": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["cleric"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 20,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_CLERIC_HEAL_MATRICES + EXACT_CLERIC_CURE_MATRICES + EXACT_CLERIC_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("focus", 1077, 8900, MIGHT_RECIPIENTS, (1077, 1308)),
			exact_support_buff("acumen", 1085, 9000, CASTING_RECIPIENTS, (1002, 1004, 1085)),
			exact_support_buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("berserker-spirit", 1062, 8800, BERSERKER_SPIRIT_RECIPIENTS, (1062,)),
			exact_support_buff("regeneration", 1044, 8700, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			# Holy Weapon, Kiss of Eva, and Resist Fire remain encounter- or
			# environment-gated rather than consuming finite buff slots by default.
		),
	},
	"oracle": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["oracle"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 20,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_ORACLE_RECHARGE_MATRICES + EXACT_ORACLE_HEAL_MATRICES + EXACT_ORACLE_CURE_MATRICES + EXACT_ORACLE_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("resist-poison", 1033, 9600, ALL_PLAYABLE_CLASSES, (1033,)),
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("agility", 1087, 8800, AGILITY_RECIPIENTS, (1087, 1252)),
			exact_support_buff("regeneration", 1044, 8700, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			exact_support_buff("decrease-weight", 1257, 8600, DECREASE_WEIGHT_RECIPIENTS, (1257,)),
			# Holy Weapon and Kiss of Eva require encounter or environment evidence.
		),
	},
	"shillienOracle": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["shillienOracle"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 20,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_SHILLIEN_ORACLE_RECHARGE_MATRICES + EXACT_SHILLIEN_ORACLE_HEAL_MATRICES + EXACT_SHILLIEN_ORACLE_CURE_MATRICES + EXACT_SHILLIEN_ORACLE_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("focus", 1077, 8900, MIGHT_RECIPIENTS, (1077, 1308)),
			exact_support_buff("vampiric-rage", 1268, 8800, VAMPIRIC_RAGE_RECIPIENTS, (1268, 1310)),
			exact_support_buff("empower", 1059, 9000, EMPOWER_RECIPIENTS, (1059,)),
			exact_support_buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			# Resist Wind and Kiss of Eva require encounter or environment evidence.
		),
	},
	"bishop": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["bishop"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 0,
		},
		"skills": BISHOP_PARTY_WIDE_POLICIES,
		"targeted": EXACT_BISHOP_HEAL_MATRICES + EXACT_BISHOP_CURE_MATRICES + EXACT_BISHOP_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("focus", 1077, 8900, MIGHT_RECIPIENTS, (1077, 1308)),
			exact_support_buff("acumen", 1085, 9000, CASTING_RECIPIENTS, (1002, 1004, 1085)),
			exact_support_buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("berserker-spirit", 1062, 8800, BERSERKER_SPIRIT_RECIPIENTS, (1062,)),
			exact_support_buff("regeneration", 1044, 8700, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			# Holy Weapon, Kiss of Eva, and Resist Fire require encounter or
			# environment evidence instead of consuming finite buff slots by default.
		),
	},
	"cardinal": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["cardinal"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 0,
		},
		"skills": CARDINAL_PARTY_WIDE_POLICIES,
		"targeted": EXACT_CARDINAL_HEAL_MATRICES + EXACT_CARDINAL_INHERITED_CURE_MATRICES + EXACT_CARDINAL_CLEANSE_MATRICES + EXACT_CARDINAL_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("divine-protection", 1353, 9800, ALL_PLAYABLE_CLASSES, (1353,)),
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("focus", 1077, 8900, MIGHT_RECIPIENTS, (1077, 1308)),
			exact_support_buff("acumen", 1085, 9000, CASTING_RECIPIENTS, (1002, 1004, 1085)),
			exact_support_buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("berserker-spirit", 1062, 8800, BERSERKER_SPIRIT_RECIPIENTS, (1062,)),
			exact_support_buff("regeneration", 1044, 8700, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			# Holy Weapon, Kiss of Eva, and Resist Fire require encounter or
			# environment evidence instead of consuming finite buff slots by default.
		),
	},
	"elder": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["elder"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 50,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_ELDER_RECHARGE_MATRICES + EXACT_ELDER_HEAL_MATRICES + EXACT_ELDER_VITALIZE_HEAL_MATRICES + EXACT_ELDER_CURE_MATRICES + EXACT_ELDER_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("resist-shock", 1259, 9700, ALL_PLAYABLE_CLASSES, (1259,)),
			exact_support_buff("resist-poison", 1033, 9600, ALL_PLAYABLE_CLASSES, (1033,)),
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9100, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("concentration", 1078, 9000, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("agility", 1087, 8900, AGILITY_RECIPIENTS, (1087, 1252)),
			exact_support_buff("wild-magic", 1303, 8800, WILD_MAGIC_RECIPIENTS, (1303,)),
			exact_support_buff("clarity", 1397, 8700, ALL_PLAYABLE_CLASSES, (1397,)),
			exact_support_buff("bless-shield", 1243, 8600, SHIELD_BLOCK_RECIPIENTS, (1243, 1250)),
			exact_support_buff("advanced-block", 1304, 8500, SHIELD_BLOCK_RECIPIENTS, (1304,)),
			exact_support_buff("regeneration", 1044, 8400, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			exact_support_buff("decrease-weight", 1257, 8300, DECREASE_WEIGHT_RECIPIENTS, (1257,)),
			# Holy Weapon, Kiss of Eva, and Unholy Resistance require encounter
			# or environment evidence. Return and Party Recall belong to travel.
		),
	},
	"shillenElder": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["shillenElder"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 40,
		},
		"skills": (),
		"targeted": EXACT_RECHARGE_MATRICES + EXACT_SHILLIEN_HEAL_MATRICES,
		"buffs": (
			buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			buff("resist-wind", 1189, 9200, ALL_PLAYABLE_CLASSES, (1189,)),
			buff("holy-resistance", 1392, 9100, ALL_PLAYABLE_CLASSES, (1392,)),
			buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			buff("focus", 1077, 8900, MIGHT_RECIPIENTS, (1077, 1308)),
			buff("death-whisper", 1242, 8800, MIGHT_RECIPIENTS, (1242, 1253, 1364)),
			buff("guidance", 1240, 8700, MIGHT_RECIPIENTS, (1240, 1249, 1309)),
			buff("vampiric-rage", 1268, 8600, VAMPIRIC_RAGE_RECIPIENTS, (1268, 1310)),
			buff("empower", 1059, 9000, EMPOWER_RECIPIENTS, (1059,)),
			buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			buff("wild-magic", 1303, 8800, WILD_MAGIC_RECIPIENTS, (1303,)),
		),
	},
	"evaSaint": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["evaSaint"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 30,
		},
		"skills": NOVICE_GROUP_HEAL_POLICIES,
		"targeted": EXACT_EVA_SAINT_RECHARGE_MATRICES + EXACT_EVA_SAINT_HEAL_MATRICES + EXACT_EVA_SAINT_VITALIZE_HEAL_MATRICES + EXACT_EVA_SAINT_CURE_MATRICES + EXACT_EVA_SAINT_RESURRECTION_MATRICES,
		"buffs": (
			exact_support_buff("prophecy-of-water", 1355, 10000, PROPHECY_OF_WATER_RECIPIENTS, (1355, 1356, 1357, 1363, 1413, 1414), 45),
			exact_support_buff("arcane-protection", 1354, 9900, ALL_PLAYABLE_CLASSES, (1354,)),
			exact_support_buff("divine-protection", 1353, 9800, ALL_PLAYABLE_CLASSES, (1353,)),
			exact_support_buff("resist-shock", 1259, 9700, ALL_PLAYABLE_CLASSES, (1259,)),
			exact_support_buff("resist-poison", 1033, 9600, ALL_PLAYABLE_CLASSES, (1033,)),
			exact_support_buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			exact_support_buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			exact_support_buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			exact_support_buff("might", 1068, 9100, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			exact_support_buff("concentration", 1078, 9000, CASTING_RECIPIENTS, (1078,)),
			exact_support_buff("agility", 1087, 8900, AGILITY_RECIPIENTS, (1087, 1252)),
			exact_support_buff("wild-magic", 1303, 8800, WILD_MAGIC_RECIPIENTS, (1303,)),
			exact_support_buff("clarity", 1397, 8700, ALL_PLAYABLE_CLASSES, (1397,)),
			exact_support_buff("bless-shield", 1243, 8600, SHIELD_BLOCK_RECIPIENTS, (1243, 1250)),
			exact_support_buff("advanced-block", 1304, 8500, SHIELD_BLOCK_RECIPIENTS, (1304,)),
			exact_support_buff("regeneration", 1044, 8400, SHIELD_BLOCK_RECIPIENTS, (1044,)),
			exact_support_buff("decrease-weight", 1257, 8300, DECREASE_WEIGHT_RECIPIENTS, (1257,)),
			# Holy Weapon, Kiss of Eva, and Unholy Resistance require encounter
			# or environment evidence. Return and Party Recall belong to travel.
			# Mass Recharge requires coordinated Spell Force and a Magic Symbol.
		),
	},
	"shillienSaint": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["shillienSaint"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 20,
		},
		"skills": (),
		"targeted": EXACT_RECHARGE_MATRICES + EXACT_SHILLIEN_HEAL_MATRICES,
		"buffs": (
			buff("prophecy-of-wind", 1357, 10000, PROPHECY_OF_WIND_RECIPIENTS, (1355, 1356, 1357, 1363, 1413, 1414)),
			buff("arcane-protection", 1354, 9900, ALL_PLAYABLE_CLASSES, (1354,)),
			buff("shield", 1040, 9500, ALL_PLAYABLE_CLASSES, (1005, 1009, 1010, 1040)),
			buff("mental-shield", 1035, 9400, ALL_PLAYABLE_CLASSES, (1035,)),
			buff("wind-walk", 1204, 9300, ALL_PLAYABLE_CLASSES, (230, 1204, 1282, 1359, 1361)),
			buff("resist-wind", 1189, 9200, ALL_PLAYABLE_CLASSES, (1189,)),
			buff("holy-resistance", 1392, 9100, ALL_PLAYABLE_CLASSES, (1392,)),
			buff("might", 1068, 9000, MIGHT_RECIPIENTS, (1003, 1007, 1068)),
			buff("focus", 1077, 8900, MIGHT_RECIPIENTS, (1077, 1308)),
			buff("death-whisper", 1242, 8800, MIGHT_RECIPIENTS, (1242, 1253, 1364)),
			buff("guidance", 1240, 8700, MIGHT_RECIPIENTS, (1240, 1249, 1309)),
			buff("vampiric-rage", 1268, 8600, VAMPIRIC_RAGE_RECIPIENTS, (1268, 1310)),
			buff("empower", 1059, 9000, EMPOWER_RECIPIENTS, (1059,)),
			buff("concentration", 1078, 8900, CASTING_RECIPIENTS, (1078,)),
			buff("wild-magic", 1303, 8800, WILD_MAGIC_RECIPIENTS, (1303,)),
		),
	},
	"swordSinger": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["swordSinger"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 100,
		},
		"skills": (),
		"group_effects": SWORD_SINGER_GROUP_EFFECTS,
	},
	"swordMuse": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["swordMuse"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 100,
		},
		"skills": (),
		"group_effects": SWORD_SINGER_GROUP_EFFECTS + (
			group_effect("song-of-renewal", 349, "SONG", 11000, CASTING_RECIPIENTS),
			group_effect("song-of-meditation", 363, "SONG", 10900, CASTING_RECIPIENTS),
			group_effect("song-of-champion", 364, "SONG", 10800, SONG_OF_CHAMPION_RECIPIENTS),
		),
	},
	"bladedancer": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["bladedancer"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 100,
		},
		"skills": (),
		"group_effects": BLADEDANCER_GROUP_EFFECTS,
	},
	"spectralDancer": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["spectralDancer"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 100,
		},
		"skills": (),
		"group_effects": SPECTRAL_DANCER_GROUP_EFFECTS,
	},
	"warcryer": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["warcryer"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 80,
		},
		"skills": (),
		"group_effects": EXACT_WARCRYER_GROUP_EFFECTS + WARCRYER_GROUP_RECOVERY_EFFECTS,
	},
	"doomcryer": {
		"profile_name": "world",
		"relationships": EXACT_CLASS_RELATIONSHIP_MATRICES["doomcryer"],
		"combat": {
			"targeting": "PARTY_ASSIST",
			"acquisition_range": 1200,
			"maximum_chase_distance": 1800,
			"preferred_range": 40,
			"basic_attack_priority": 70,
		},
		"skills": (),
		"group_effects": EXACT_DOOMCRYER_GROUP_EFFECTS + DOOMCRYER_GROUP_RECOVERY_EFFECTS,
	},
}
