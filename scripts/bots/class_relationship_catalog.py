"""Complete exact-ClassId knowledge used by every native bot profile.

The runtime never interprets a role, race, class parent, or archetype.  This
authoring module materializes an independent 89-row matrix for each supported
exact bot class.  Shared arithmetic keeps the source reviewable; the compiler
still emits every final bot-class-by-observed-class value into that profile's
XML.
"""

from typing import NamedTuple


class ExactClassRelationship(NamedTuple):
	"""One bot class's priorities when it observes one exact player class."""

	ally_support_priority: int
	ally_protection_priority: int
	hostile_attack_priority: int
	hostile_threat_priority: int
	hostile_control_priority: int
	hostile_interrupt_priority: int


class ExactProfilePerspective(NamedTuple):
	"""Six independent weights owned by one exact bot class."""

	ally_support_percent: int
	ally_protection_percent: int
	hostile_attack_percent: int
	hostile_threat_percent: int
	hostile_control_percent: int
	hostile_interrupt_percent: int


class ExactRelationshipAdjustment(NamedTuple):
	"""One explicitly authored exact source-to-observed matchup adjustment."""

	ally_support: int = 0
	ally_protection: int = 0
	hostile_attack: int = 0
	hostile_threat: int = 0
	hostile_control: int = 0
	hostile_interrupt: int = 0


# Every row is an exact Interlude ClassId, including each progression stage.
# Repeated numbers are deliberate decisions, not inheritance or role fallback.
EXACT_OBSERVED_CLASS_KNOWLEDGE = {
	"fighter": ExactClassRelationship(2000, 1900, 1700, 1700, 1500, 800),
	"warrior": ExactClassRelationship(2500, 2300, 2200, 2300, 1900, 900),
	"gladiator": ExactClassRelationship(2600, 2400, 2600, 3000, 2500, 1000),
	"warlord": ExactClassRelationship(3100, 3300, 3000, 3500, 3500, 1300),
	"knight": ExactClassRelationship(4000, 3600, 1500, 2100, 2300, 900),
	"paladin": ExactClassRelationship(4100, 3900, 1900, 2700, 2700, 1900),
	"darkAvenger": ExactClassRelationship(4200, 3900, 2300, 3400, 3300, 1800),
	"rogue": ExactClassRelationship(1500, 1800, 2400, 2800, 2200, 800),
	"treasureHunter": ExactClassRelationship(1550, 2000, 3500, 4300, 3600, 900),
	"hawkeye": ExactClassRelationship(1900, 2200, 3400, 3900, 3400, 900),
	"mage": ExactClassRelationship(1900, 2300, 2300, 2400, 2700, 3000),
	"wizard": ExactClassRelationship(1950, 2400, 2700, 3000, 3200, 3400),
	"sorceror": ExactClassRelationship(2000, 2600, 3600, 4100, 4200, 4400),
	"necromancer": ExactClassRelationship(2200, 2900, 4200, 4700, 4700, 4600),
	"warlock": ExactClassRelationship(2350, 3000, 3900, 4300, 4300, 4000),
	"cleric": ExactClassRelationship(3500, 4100, 4300, 2700, 4200, 4700),
	"bishop": ExactClassRelationship(3700, 4700, 4900, 3100, 5000, 5400),
	"prophet": ExactClassRelationship(3300, 4000, 4500, 3000, 4400, 4700),
	"elvenFighter": ExactClassRelationship(2000, 1900, 1700, 1700, 1500, 800),
	"elvenKnight": ExactClassRelationship(4000, 3600, 1500, 2100, 2300, 900),
	"templeKnight": ExactClassRelationship(4150, 4000, 1900, 2900, 3200, 2100),
	"swordSinger": ExactClassRelationship(3100, 3500, 2800, 3100, 3600, 2400),
	"elvenScout": ExactClassRelationship(1500, 1800, 2400, 2800, 2200, 800),
	"plainsWalker": ExactClassRelationship(1550, 2000, 3500, 4200, 3600, 900),
	"silverRanger": ExactClassRelationship(1900, 2200, 3400, 3900, 3400, 900),
	"elvenMage": ExactClassRelationship(1900, 2300, 2300, 2400, 2700, 3000),
	"elvenWizard": ExactClassRelationship(1950, 2400, 2700, 3000, 3200, 3400),
	"spellsinger": ExactClassRelationship(2000, 2600, 3700, 4200, 4300, 4600),
	"elementalSummoner": ExactClassRelationship(2350, 3000, 3900, 4200, 4300, 4000),
	"oracle": ExactClassRelationship(3500, 4200, 4300, 2700, 4300, 4800),
	"elder": ExactClassRelationship(3700, 4700, 4800, 3100, 4900, 5300),
	"darkFighter": ExactClassRelationship(2050, 1950, 1800, 1800, 1600, 800),
	"palusKnight": ExactClassRelationship(4050, 3700, 1700, 2400, 2700, 1100),
	"shillienKnight": ExactClassRelationship(4200, 4000, 2300, 3500, 3700, 2100),
	"bladedancer": ExactClassRelationship(3150, 3500, 3100, 3500, 3600, 2200),
	"assassin": ExactClassRelationship(1500, 1800, 2500, 3000, 2300, 800),
	"abyssWalker": ExactClassRelationship(1550, 2000, 3600, 4400, 3700, 900),
	"phantomRanger": ExactClassRelationship(1900, 2200, 3500, 4100, 3500, 900),
	"darkMage": ExactClassRelationship(1900, 2300, 2300, 2500, 2800, 3100),
	"darkWizard": ExactClassRelationship(1950, 2400, 2800, 3200, 3300, 3500),
	"spellhowler": ExactClassRelationship(2050, 2600, 3900, 4500, 4400, 4700),
	"phantomSummoner": ExactClassRelationship(2350, 3000, 4000, 4400, 4400, 4100),
	"shillienOracle": ExactClassRelationship(3500, 4200, 4400, 2800, 4400, 4900),
	"shillenElder": ExactClassRelationship(3700, 4700, 4900, 3200, 5000, 5400),
	"orcFighter": ExactClassRelationship(2300, 2200, 2100, 2300, 1900, 800),
	"orcRaider": ExactClassRelationship(2800, 2700, 2600, 3100, 2400, 900),
	"destroyer": ExactClassRelationship(3200, 3200, 3300, 4300, 3300, 1000),
	"orcMonk": ExactClassRelationship(2300, 2200, 2400, 2900, 2200, 800),
	"tyrant": ExactClassRelationship(2700, 2600, 3400, 4100, 3500, 1100),
	"orcMage": ExactClassRelationship(2900, 3300, 3200, 3000, 3400, 3900),
	"orcShaman": ExactClassRelationship(3350, 3900, 4100, 3300, 4200, 4600),
	"overlord": ExactClassRelationship(3400, 4200, 4700, 3900, 4900, 5000),
	"warcryer": ExactClassRelationship(3350, 4000, 4500, 3600, 4500, 4800),
	"dwarvenFighter": ExactClassRelationship(2500, 2400, 1900, 2100, 1800, 800),
	"scavenger": ExactClassRelationship(2550, 2500, 2100, 2400, 2100, 900),
	"bountyHunter": ExactClassRelationship(2600, 2700, 2500, 3000, 2800, 1000),
	"artisan": ExactClassRelationship(2550, 2600, 2100, 2400, 2200, 1000),
	"warsmith": ExactClassRelationship(2850, 3100, 3000, 3400, 3200, 1800),
	"duelist": ExactClassRelationship(2700, 2600, 3500, 4100, 3500, 1200),
	"dreadnought": ExactClassRelationship(3250, 3500, 3900, 4700, 4400, 1700),
	"phoenixKnight": ExactClassRelationship(4300, 4400, 2300, 3400, 3500, 2500),
	"hellKnight": ExactClassRelationship(4400, 4400, 2900, 4500, 4400, 2500),
	"sagittarius": ExactClassRelationship(1950, 2500, 4100, 4700, 4000, 1200),
	"adventurer": ExactClassRelationship(1600, 2300, 4300, 5200, 4400, 1200),
	"archmage": ExactClassRelationship(2050, 2900, 4500, 5000, 4900, 5200),
	"soultaker": ExactClassRelationship(2250, 3200, 5100, 5600, 5500, 5400),
	"arcanaLord": ExactClassRelationship(2400, 3300, 4700, 5100, 5000, 4700),
	"cardinal": ExactClassRelationship(3900, 5400, 5600, 3600, 5700, 6000),
	"hierophant": ExactClassRelationship(3450, 4600, 5100, 3700, 5100, 5400),
	"evaTemplar": ExactClassRelationship(4350, 4500, 2400, 3700, 4000, 2800),
	"swordMuse": ExactClassRelationship(3250, 4100, 3600, 3900, 4500, 3200),
	"windRider": ExactClassRelationship(1600, 2300, 4300, 5100, 4400, 1200),
	"moonlightSentinel": ExactClassRelationship(1950, 2500, 4200, 4800, 4100, 1200),
	"mysticMuse": ExactClassRelationship(2050, 2900, 4600, 5100, 5000, 5400),
	"elementalMaster": ExactClassRelationship(2400, 3300, 4700, 5000, 5000, 4700),
	"evaSaint": ExactClassRelationship(3900, 5300, 5500, 3500, 5600, 5900),
	"shillienTemplar": ExactClassRelationship(4400, 4500, 3000, 4700, 4700, 2900),
	"spectralDancer": ExactClassRelationship(3250, 4100, 4000, 4400, 4600, 3200),
	"ghostHunter": ExactClassRelationship(1600, 2300, 4400, 5300, 4500, 1200),
	"ghostSentinel": ExactClassRelationship(1950, 2500, 4300, 5000, 4200, 1200),
	"stormScreamer": ExactClassRelationship(2100, 2900, 4800, 5400, 5200, 5600),
	"spectralMaster": ExactClassRelationship(2400, 3300, 4800, 5200, 5100, 4800),
	"shillienSaint": ExactClassRelationship(3850, 5200, 5400, 3500, 5500, 5800),
	"titan": ExactClassRelationship(3300, 3500, 4000, 5200, 4100, 1200),
	"grandKhauatari": ExactClassRelationship(2750, 2900, 4100, 5000, 4200, 1400),
	"dominator": ExactClassRelationship(3500, 4700, 5400, 4500, 5600, 5700),
	"doomcryer": ExactClassRelationship(3450, 4500, 5200, 4200, 5200, 5500),
	"fortuneSeeker": ExactClassRelationship(2650, 3000, 3100, 3500, 3300, 1300),
	"maestro": ExactClassRelationship(2950, 3500, 3700, 4100, 3900, 2300),
}


# Each currently supported exact class owns its perspective.  Progressions are
# deliberately repeated rather than inherited from their parent profile.
EXACT_PROFILE_PERSPECTIVES = {
	"hierophant": ExactProfilePerspective(102, 105, 95, 105, 108, 110),
	"prophet": ExactProfilePerspective(100, 102, 95, 103, 105, 107),
	"cleric": ExactProfilePerspective(106, 106, 85, 112, 100, 105),
	"oracle": ExactProfilePerspective(105, 108, 82, 115, 105, 110),
	"shillienOracle": ExactProfilePerspective(104, 106, 90, 115, 108, 108),
	"bishop": ExactProfilePerspective(112, 112, 70, 120, 105, 112),
	"cardinal": ExactProfilePerspective(116, 116, 65, 125, 110, 118),
	"elder": ExactProfilePerspective(110, 110, 78, 118, 108, 114),
	"shillenElder": ExactProfilePerspective(108, 108, 88, 118, 110, 112),
	"evaSaint": ExactProfilePerspective(114, 114, 75, 122, 112, 118),
	"shillienSaint": ExactProfilePerspective(112, 112, 85, 122, 114, 116),
	"swordSinger": ExactProfilePerspective(100, 112, 105, 100, 112, 105),
	"swordMuse": ExactProfilePerspective(103, 118, 108, 102, 118, 110),
	"bladedancer": ExactProfilePerspective(100, 108, 112, 108, 110, 105),
	"spectralDancer": ExactProfilePerspective(102, 112, 118, 112, 116, 110),
	"warcryer": ExactProfilePerspective(104, 108, 110, 108, 108, 105),
	"doomcryer": ExactProfilePerspective(108, 112, 115, 112, 112, 110),
}


def adjustment(ally_support=0, ally_protection=0, hostile_attack=0, hostile_threat=0, hostile_control=0, hostile_interrupt=0):
	return ExactRelationshipAdjustment(ally_support, ally_protection, hostile_attack, hostile_threat, hostile_control, hostile_interrupt)


# These pair-specific decisions change rankings that a source-wide perspective
# alone cannot express.  They are keyed only by exact source and target ClassId.
EXACT_MATCHUP_ADJUSTMENTS = {
	"hierophant": {
		"cardinal": adjustment(300, 350, 300, 0, 300, 300),
		"phoenixKnight": adjustment(250, 200),
		"abyssWalker": adjustment(hostile_threat=350, hostile_control=150),
		"dominator": adjustment(hostile_attack=250, hostile_control=300, hostile_interrupt=250),
	},
	"prophet": {
		"bishop": adjustment(250, 300, 250, 0, 250, 250),
		"darkAvenger": adjustment(200, 150),
		"treasureHunter": adjustment(hostile_threat=300, hostile_control=150),
		"overlord": adjustment(hostile_attack=200, hostile_control=250, hostile_interrupt=200),
	},
	"cleric": {
		"cardinal": adjustment(400, 400, 200, 0, 200, 250),
		"phoenixKnight": adjustment(350, 250),
		"hellKnight": adjustment(350, 250),
		"abyssWalker": adjustment(hostile_threat=400, hostile_control=200),
	},
	"oracle": {
		"evaSaint": adjustment(400, 400, 200, 0, 250, 300),
		"mysticMuse": adjustment(300, 150),
		"evaTemplar": adjustment(350, 250),
		"windRider": adjustment(hostile_threat=400, hostile_control=200),
	},
	"shillienOracle": {
		"shillienSaint": adjustment(400, 400, 200, 0, 250, 300),
		"stormScreamer": adjustment(300, 150),
		"shillienTemplar": adjustment(350, 250),
		"ghostHunter": adjustment(hostile_threat=400, hostile_control=200),
	},
	"bishop": {
		"cardinal": adjustment(450, 450, 150, 0, 250, 300),
		"phoenixKnight": adjustment(450, 300),
		"hellKnight": adjustment(450, 300),
		"adventurer": adjustment(hostile_threat=450, hostile_control=250),
	},
	"cardinal": {
		"bishop": adjustment(450, 500, 150, 0, 250, 300),
		"evaSaint": adjustment(400, 450, 150, 0, 250, 300),
		"phoenixKnight": adjustment(500, 350),
		"ghostHunter": adjustment(hostile_threat=500, hostile_control=300),
	},
	"elder": {
		"evaSaint": adjustment(400, 450, 200, 0, 250, 300),
		"mysticMuse": adjustment(350, 200),
		"evaTemplar": adjustment(400, 300),
		"windRider": adjustment(hostile_threat=450, hostile_control=250),
	},
	"shillenElder": {
		"shillienSaint": adjustment(400, 450, 200, 0, 250, 300),
		"stormScreamer": adjustment(350, 200),
		"shillienTemplar": adjustment(400, 300),
		"ghostHunter": adjustment(hostile_threat=450, hostile_control=250),
	},
	"evaSaint": {
		"cardinal": adjustment(400, 500, 200, 0, 300, 350),
		"mysticMuse": adjustment(400, 250),
		"evaTemplar": adjustment(500, 350),
		"adventurer": adjustment(hostile_threat=500, hostile_control=300),
	},
	"shillienSaint": {
		"cardinal": adjustment(400, 500, 200, 0, 300, 350),
		"stormScreamer": adjustment(400, 250),
		"shillienTemplar": adjustment(500, 350),
		"ghostHunter": adjustment(hostile_threat=500, hostile_control=300),
	},
	"swordSinger": {
		"bishop": adjustment(150, 500),
		"elder": adjustment(150, 450),
		"spellhowler": adjustment(hostile_attack=250, hostile_threat=300, hostile_control=250, hostile_interrupt=250),
		"abyssWalker": adjustment(hostile_attack=150, hostile_threat=300),
	},
	"swordMuse": {
		"cardinal": adjustment(200, 550),
		"evaSaint": adjustment(200, 500),
		"stormScreamer": adjustment(hostile_attack=300, hostile_threat=350, hostile_control=300, hostile_interrupt=300),
		"ghostHunter": adjustment(hostile_attack=200, hostile_threat=350),
	},
	"bladedancer": {
		"bishop": adjustment(150, 500),
		"shillenElder": adjustment(150, 450),
		"spellsinger": adjustment(hostile_attack=250, hostile_threat=300, hostile_control=250, hostile_interrupt=250),
		"plainsWalker": adjustment(hostile_attack=150, hostile_threat=300),
	},
	"spectralDancer": {
		"cardinal": adjustment(200, 550),
		"shillienSaint": adjustment(200, 500),
		"mysticMuse": adjustment(hostile_attack=300, hostile_threat=350, hostile_control=300, hostile_interrupt=300),
		"windRider": adjustment(hostile_attack=200, hostile_threat=350),
	},
	"warcryer": {
		"bishop": adjustment(250, 350, 250, 0, 250, 250),
		"destroyer": adjustment(300, 250),
		"tyrant": adjustment(250, 200),
		"necromancer": adjustment(hostile_attack=250, hostile_control=300, hostile_interrupt=250),
	},
	"doomcryer": {
		"cardinal": adjustment(300, 400, 300, 0, 300, 300),
		"titan": adjustment(350, 300),
		"grandKhauatari": adjustment(300, 250),
		"soultaker": adjustment(hostile_attack=300, hostile_control=350, hostile_interrupt=300),
	},
}


MINIMUM_PRIORITY = 1
MAXIMUM_PRIORITY = 6000


def _weighted(value, percent, delta):
	weighted = ((value * percent) + 50) // 100
	return max(MINIMUM_PRIORITY, min(MAXIMUM_PRIORITY, weighted + delta))


def exact_relationship_matrix(source_class):
	"""Materialize one independent complete matrix for an exact bot class."""
	view = EXACT_PROFILE_PERSPECTIVES[source_class]
	adjustments = EXACT_MATCHUP_ADJUSTMENTS[source_class]
	matrix = {}
	for observed_class, knowledge in EXACT_OBSERVED_CLASS_KNOWLEDGE.items():
		delta = adjustments.get(observed_class, ExactRelationshipAdjustment())
		matrix[observed_class] = ExactClassRelationship(
			_weighted(knowledge.ally_support_priority, view.ally_support_percent, delta.ally_support),
			_weighted(knowledge.ally_protection_priority, view.ally_protection_percent, delta.ally_protection),
			_weighted(knowledge.hostile_attack_priority, view.hostile_attack_percent, delta.hostile_attack),
			_weighted(knowledge.hostile_threat_priority, view.hostile_threat_percent, delta.hostile_threat),
			_weighted(knowledge.hostile_control_priority, view.hostile_control_percent, delta.hostile_control),
			_weighted(knowledge.hostile_interrupt_priority, view.hostile_interrupt_percent, delta.hostile_interrupt),
		)
	return matrix


EXACT_CLASS_RELATIONSHIP_MATRICES = {
	source_class: exact_relationship_matrix(source_class)
	for source_class in EXACT_PROFILE_PERSPECTIVES
}
