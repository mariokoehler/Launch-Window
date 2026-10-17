"""Patch definitions for the standard pack's goals beyond the early era (2026-10-08), imported by
make_patches.py. Same fields as its GOALS: ring text (top, bottom), colors and center artwork, written
from the real hardware and missions, never from BASPM's patches (design.md 1.2). Ring texts stay short
and plain: long ones are where the model misspells.

The many "<program> <mission> flown" goals share scene templates per kind of mission, each filled with
a description of the program's own craft.
"""

NASA = dict(ring='dark navy blue', edge='gold', letters='white', field='deep navy-blue space',
            palette='navy blue, gold, white, sky blue and silver')
SOVIET = dict(ring='deep scarlet red', edge='gold', letters='gold', field='deep black space',
              palette='scarlet red, gold, white, black and silver')
STYLE = 'in flat vintage 1960s vector-illustration style'


def custom(goal_id, top, bottom, ring, edge, letters, field, palette, art):
    return dict(id=goal_id, top=top, bottom=bottom, ring=ring, edge=edge, letters=letters, field=field,
                palette=palette, art=art)


def side(goal_id, top, bottom, agency, art):
    return dict(id=goal_id, top=top, bottom=bottom, **agency, art=art)


# The crewed craft, as the artwork describes them.
GEMINI = ('the Gemini capsule: a black two-seat cone-shaped capsule with two hatch windows, joined at its base to '
          'a wider white adapter section')
VOSKHOD = ('the Voskhod spacecraft: a round spherical descent module with a small round porthole, sitting on a '
           'double-cone equipment module, with a small extra rocket pack on top of the sphere')
SOYUZ_L1 = ('a Soyuz spacecraft: a bell-shaped green descent module on a cylindrical service module with two dark '
            'blue solar panel wings')
SOYUZ_LOK = ('a Soyuz spacecraft: a round green orbital module at its front, a bell-shaped descent module and a '
             'cylindrical service module with two dark blue solar panel wings')
SOYUZ_OK = ('a Soyuz spacecraft: a round green orbital module with a docking probe at its nose, a bell-shaped '
            'descent module and a cylindrical service module with two dark blue solar panel wings')
LK700 = 'a large, heavy cone-shaped crewed spacecraft stacked on a wide landing stage with four folded legs'
APOLLO = ('the Apollo spacecraft: a silver cone-shaped command module on a long cylindrical white-and-silver '
          'service module with a bell-shaped engine nozzle')
GEMINI_DA = ('a tall Gemini lunar craft: a black Gemini capsule on top of a stack of cylindrical landing and ascent '
             'stages with four landing legs')
GEMINI_LUNAR = ('a black Gemini capsule joined to a cylindrical propulsion module with a bell-shaped engine')
GEMINI_EOR = ('a black Gemini capsule docked to a long cylindrical service module, with a small open lunar lander '
              'attached behind it')
LM = ('the Apollo lunar module: a gold-foil-wrapped lower stage with four spindly legs and an angular gray upper '
      'cabin')
LK = 'the LK lunar lander: a small one-person lander with a round crew cabin on a four-legged landing platform'
LANGLEY = ('a minimal open lunar lander: a small platform with four legs, round propellant tanks and one seat for '
           'an astronaut in a white spacesuit')


def uncrewed_suborbital(goal_id, top, agency, craft):
    return side(goal_id, top, 'UNCREWED SUBORBITAL TEST', agency,
                f'{craft}, {STYLE}, rising on a high arc-shaped trajectory line that climbs from the Earth and falls '
                f'back to it, without ever circling the planet.')


def suborbital(goal_id, top, agency, craft):
    return side(goal_id, top, 'CREWED SUBORBITAL FLIGHT', agency,
                f'{craft}, {STYLE}, at the top of a high arc-shaped trajectory line that climbs from the Earth and '
                f'falls back to it. A small white pilot helmet emblem shines above it.')


def uncrewed_orbital(goal_id, top, agency, craft):
    return side(goal_id, top, 'UNCREWED ORBITAL TEST', agency,
                f'{craft}, {STYLE}, travelling along a full elliptical orbit line that circles all the way around a '
                f'blue Earth in the center of the patch.')


def orbital(goal_id, top, agency, craft):
    return side(goal_id, top, 'CREWED ORBITAL FLIGHT', agency,
                f'{craft}, {STYLE}, circling a blue Earth on a full elliptical orbit line. A small rising sun peeks '
                f'over the edge of the Earth.')


def spacewalk(goal_id, top, agency, craft):
    return side(goal_id, top, 'SPACEWALK', agency,
                f'{craft}, {STYLE}, above the curved blue limb of the Earth, with a tiny figure in a white spacesuit '
                f'floating beside it on a long golden tether.')


def uncrewed_flyby(goal_id, top, agency, craft):
    return side(goal_id, top, 'UNCREWED LUNAR FLYBY', agency,
                f'{craft}, {STYLE}, swinging around a gray cratered Moon on a thin white figure-eight path that '
                f'loops back to a small blue Earth in the lower corner.')


def flyby(goal_id, top, agency, craft):
    return side(goal_id, top, 'LUNAR FLYBY', agency,
                f'{craft}, {STYLE}, swinging around a large gray cratered Moon on a thin white figure-eight path '
                f'that loops back to a small blue Earth. A small white pilot helmet emblem shines above it.')


def uncrewed_lunar_orbit(goal_id, top, agency, craft):
    return side(goal_id, top, 'UNCREWED LUNAR ORBIT', agency,
                f'{craft}, {STYLE}, circling a large gray cratered Moon on a full elliptical orbit line, with a '
                f'small blue Earth rising behind the Moon.')


def lunar_orbit(goal_id, top, agency, craft):
    return side(goal_id, top, 'LUNAR ORBIT', agency,
                f'{craft}, {STYLE}, circling a large gray cratered Moon on a full elliptical orbit line, with a '
                f'blue-and-white Earth rising over the Moon\'s horizon.')


def lander_test(goal_id, top, agency, craft, lander):
    return side(goal_id, top, 'LANDER TEST', agency,
                f'{craft} and {lander} separating from each other above the gray cratered surface of the Moon, '
                f'{STYLE}, with a thin dotted line showing the lander\'s path down and back up.')


def landing(goal_id, top, agency, lander):
    return side(goal_id, top, 'MOON LANDING', agency,
                f'{lander} standing on the gray cratered surface of the Moon, {STYLE}, with a small figure in a '
                f'white spacesuit beside it and a blue-and-white Earth hanging in the black sky above.')


def reentry_test(goal_id, top, agency, craft):
    return side(goal_id, top, 'LUNAR-SPEED REENTRY', agency,
                f'{craft}, {STYLE}, plunging heat-shield-first into the Earth\'s atmosphere with a long fiery '
                f'orange-and-yellow plasma trail behind it, above the curved blue limb of the Earth.')


def flown(goal_id, top, bottom, agency, art):
    return side(goal_id, top, bottom, agency, f'{art}, {STYLE}.')


FULL_GOALS = [
    # ---- Earth-orbit satellites
    flown('micrometeoroid-satellite-flown', 'PEGASUS', 'MICROMETEOROID SURVEY', NASA,
          'the Pegasus satellite: a cylindrical body with two enormous, flat, rectangular detector wings spread '
          'wide to either side, in orbit above a blue Earth, with tiny glowing specks of dust streaking past'),
    custom('micrometeoroid-study', 'MICROMETEOROIDS', 'MEASURED', 'dark slate gray', 'gold', 'white',
           'deep black space', 'slate gray, gold, white and pale blue',
           'a flat metal panel in the center, peppered with tiny craters, with small glowing specks streaking '
           'toward it from the dark, above the curved edge of the Earth, ' + STYLE + '.'),
    flown('biosatellite-flown', 'BIOSATELLITE', 'LIFE IN ORBIT', NASA,
          'a blunt cone-shaped return capsule with a round heat shield, containing a small sprouting green wheat '
          'plant and a few tiny insects in a window, orbiting above a blue Earth'),
    flown('ofo-flown', 'FROG OTOLITH', 'BALANCE IN ORBIT', NASA,
          'a small drum-shaped satellite with a round window showing two green frogs floating in water, orbiting '
          'above a blue Earth'),
    custom('amphibians-in-microgravity', 'FROGS IN ORBIT', '', 'dark green', 'gold', 'white',
           'deep teal-blue space', 'dark green, teal, gold and white',
           'two cheerful green bullfrogs floating weightlessly inside a round glass bubble, with a simple drawing of '
           'the inner ear beside them and the curved limb of the Earth below, ' + STYLE + '.'),
    # ---- Lunar probes
    custom('lunar-probe', 'PROBE TO THE MOON', '', 'slate blue', 'silver', 'white', 'deep black space',
           'slate blue, silver, white and pale gray',
           'a small generic space probe with a dish antenna flying from a small blue Earth toward a large gray '
           'cratered Moon on a thin dotted white path, ' + STYLE + '.'),
    custom('lunar-flyby', 'LUNAR FLYBY', '', 'steel blue', 'silver', 'white', 'deep black space',
           'steel blue, silver, white and gray',
           'a small generic probe curving past the edge of a large gray cratered Moon on a thin white arc, with '
           'the Earth small in the distance, ' + STYLE + '.'),
    flown('pioneer-4-flown', 'PIONEER 4', 'LUNAR FLYBY', NASA,
          'the Pioneer 4 probe: a small gold-and-white cone with a thin antenna at its tip, flying past a large '
          'gray Moon on a curved path that continues out into space toward the Sun'),
    flown('luna-3-flown', 'LUNA 3', 'FAR SIDE', SOVIET,
          'the Luna 3 probe: a silver cylinder with rounded ends and short antennas, looping around the far side '
          'of a large gray Moon, with a small camera lens and rays of light pointing at the Moon'),
    custom('lunar-photos', 'PICTURES OF THE MOON', '', 'charcoal gray', 'silver', 'white', 'deep black space',
           'charcoal gray, silver, white and pale gray',
           'a vintage camera lens in the center reflecting a gray cratered Moon, with a strip of film frames '
           'curving around it, ' + STYLE + '.'),
    flown('zond-5-flown', 'ZOND 5', 'AROUND THE MOON', SOVIET,
          'a Soyuz-style capsule swinging around a gray Moon on a figure-eight path, with a small friendly steppe '
          'tortoise in the foreground looking up at the Moon'),
    custom('lunar-bioscience', 'LIFE AROUND THE MOON', '', 'olive green', 'gold', 'white', 'deep black space',
           'olive green, gold, white and gray',
           'a small tortoise with a patterned shell sitting on a stylized crescent Moon, with a thin orbit line '
           'looping around them, ' + STYLE + '.'),
    custom('lunar-impact', 'PROBE ON THE MOON', '', 'dark bronze', 'gold', 'white', 'deep black space',
           'bronze, gold, white and gray',
           'a small probe diving toward the gray cratered surface of the Moon on a straight dotted line, with a '
           'small star-shaped burst where it meets the surface, ' + STYLE + '.'),
    flown('ranger-3-flown', 'RANGER 3', 'LUNAR IMPACTOR', NASA,
          'the Ranger probe: a hexagonal frame with two flat solar panel wings, a dish antenna and a tall camera '
          'tower, diving toward the gray surface of the Moon'),
    flown('luna-2-flown', 'LUNA 2', 'TO THE MOON', SOVIET,
          'the Luna 2 probe: a polished silver sphere with long antennas, diving toward the gray Moon on a dotted '
          'line, with a few small pentagonal metal badges scattering where it meets the surface'),
    custom('lunar-structure-study', 'THE MOON STUDIED', '', 'deep indigo', 'silver', 'white', 'deep black space',
           'indigo, silver, white and gray',
           'a cutaway of the Moon showing its gray crust and darker layers inside, with a small probe passing '
           'overhead, ' + STYLE + '.'),
    custom('lunar-soil-study', 'LUNAR SURFACE', 'STUDIED', 'warm gray', 'gold', 'white', 'deep black space',
           'warm gray, gold, white and charcoal',
           'a close view of the gray lunar surface with small craters and scattered rocks, and a magnifying glass '
           'held over it, ' + STYLE + '.'),
    custom('lunar-close-up-photos', 'MOON CLOSE-UP', '', 'charcoal gray', 'gold', 'white', 'deep black space',
           'charcoal, gold, white and gray',
           'a square photograph frame showing craters of the lunar surface in close detail, held over the curve '
           'of the Moon, ' + STYLE + '.'),
    custom('lunar-orbit', 'LUNAR ORBIT', '', 'dark teal', 'silver', 'white', 'deep black space',
           'teal, silver, white and gray',
           'a small generic probe circling a large gray cratered Moon on a full elliptical orbit line, ' + STYLE + '.'),
    flown('pioneer-2-flown', 'PIONEER 2', 'LUNAR ORBITER', NASA,
          'a small round probe with a ring of thin antennas, flying toward the Moon on a curved path'),
    custom('lunar-mapping', 'MOON MAPPED', '', 'navy blue', 'gold', 'white', 'deep navy-blue space',
           'navy, gold, white and gray',
           'a gray Moon covered in a fine grid of map lines, with a small probe circling it on an orbit line, '
           + STYLE + '.'),
    custom('lunar-structure-survey', 'MOON SURVEYED', '', 'deep indigo', 'gold', 'white', 'deep black space',
           'indigo, gold, white and gray',
           'a gray Moon with contour lines and a cutaway wedge showing its layers, circled by a probe on an orbit '
           'line, ' + STYLE + '.'),
    flown('lunar-orbiter-flown', 'LUNAR ORBITER', 'LANDING SITE SURVEY', NASA,
          'the Lunar Orbiter: a cone-shaped spacecraft with four flat solar panels and a long antenna boom, '
          'circling a gray Moon with a square camera frame pointed down at the surface'),
    custom('lunar-micrometeoroid-study', 'LUNAR DUST', 'MEASURED', 'slate gray', 'gold', 'white',
           'deep black space', 'slate gray, gold, white and pale gray',
           'a gray Moon with tiny glowing specks streaking past a small probe in orbit, ' + STYLE + '.'),
    custom('lunar-lander', 'LANDER ON THE MOON', '', 'dark bronze', 'gold', 'white', 'deep black space',
           'bronze, gold, white and gray',
           'a small three-legged robot lander standing on the gray lunar surface, with the Earth in the black sky, '
           + STYLE + '.'),
    custom('lunar-soil-analysis', 'LUNAR SOIL', 'ANALYZED', 'warm gray', 'gold', 'white', 'deep black space',
           'warm gray, gold, white and charcoal',
           'a small robotic scoop digging a neat trench in the gray lunar soil, ' + STYLE + '.'),
    custom('lunar-soft-landing', 'SOFT LANDING', '', 'royal blue', 'gold', 'white', 'deep black space',
           'royal blue, gold, white and gray',
           'a small robot lander settling gently onto the Moon on short braking flames, kicking up a little gray '
           'dust, ' + STYLE + '.'),
    custom('lunar-surface-photos', 'VIEW FROM THE MOON', '', 'charcoal gray', 'gold', 'white', 'deep black space',
           'charcoal, gold, white and gray',
           'a panoramic view across the gray lunar surface to a sharp black horizon, seen through a round camera '
           'lens, ' + STYLE + '.'),
    custom('lunar-rover', 'ROVER ON THE MOON', '', 'deep green', 'gold', 'white', 'deep black space',
           'deep green, gold, white and gray',
           'a small eight-wheeled robot rover with a round lid, rolling across the gray lunar surface and leaving '
           'wheel tracks behind it, ' + STYLE + '.'),
    custom('lunar-sample-return', 'MOON SAMPLE', 'RETURNED', 'deep maroon', 'gold', 'white', 'deep black space',
           'maroon, gold, white and gray',
           'a small round capsule rising from a robot lander on the Moon toward a small blue Earth on a dotted '
           'line, ' + STYLE + '.'),
    custom('lunar-soil-survey', 'LUNAR SOIL', 'SURVEYED', 'warm brown', 'gold', 'white', 'deep black space',
           'brown, gold, white and gray',
           'a small glass vial of gray lunar soil standing in front of a large Moon, ' + STYLE + '.'),
    custom('lunar-sample-retrieved', 'MOON SOIL ON EARTH', '', 'deep maroon', 'gold', 'white',
           'deep navy-blue space', 'maroon, gold, white and blue',
           'a small round capsule under a parachute descending over Earth\'s grassy plains, with the Moon in the '
           'sky above, ' + STYLE + '.'),

    # ---- Sun and Mercury
    custom('sun-probe', 'PROBE TO THE SUN', '', 'deep orange', 'gold', 'white', 'deep black space',
           'orange, gold, white and yellow',
           'a large glowing yellow Sun with flowing rays, and a small drum-shaped probe orbiting it on a thin white '
           'ellipse, ' + STYLE + '.'),
    custom('solar-orbit', 'SOLAR ORBIT', '', 'burnt orange', 'gold', 'white', 'deep black space',
           'burnt orange, gold, white and yellow',
           'a glowing Sun in the center with the small blue Earth and a tiny probe on their own orbit lines around '
           'it, ' + STYLE + '.'),
    custom('solar-study', 'SUN STUDIED', '', 'deep orange', 'gold', 'white', 'deep black space',
           'orange, gold, white and red',
           'the Sun with a large looping solar flare, watched by a small probe with a dish antenna, ' + STYLE + '.'),
    custom('solar-wind-measurements', 'SOLAR WIND', 'MEASURED', 'amber', 'gold', 'white', 'deep black space',
           'amber, gold, white and pale yellow',
           'streams of glowing particles flowing outward from a bright Sun past a small drum-shaped probe, '
           + STYLE + '.'),
    custom('interplanetary-magnetic-field', 'SOLAR MAGNETIC FIELD', '', 'deep purple', 'gold', 'white',
           'deep black space', 'purple, gold, white and orange',
           'the Sun with curving spiral magnetic field lines sweeping outward past a small probe, ' + STYLE + '.'),
    custom('mercury-probe', 'PROBE TO MERCURY', '', 'dark copper', 'gold', 'white', 'deep black space',
           'copper, gold, white and gray',
           'a small probe flying toward a gray cratered planet Mercury, with a huge bright Sun behind it, '
           + STYLE + '.'),
    custom('mercury-flyby', 'MERCURY FLYBY', '', 'dark copper', 'silver', 'white', 'deep black space',
           'copper, silver, white and gray',
           'a probe with two solar panels and a round sunshade passing close by the gray cratered planet Mercury, '
           + STYLE + '.'),
    custom('mercury-mapping', 'MERCURY MAPPED', '', 'charcoal gray', 'gold', 'white', 'deep black space',
           'charcoal, gold, white and gray',
           'the gray cratered planet Mercury covered with a fine grid of map lines, ' + STYLE + '.'),
    custom('mercury-structure-study', 'MERCURY STUDIED', '', 'dark copper', 'gold', 'white', 'deep black space',
           'copper, gold, white and gray',
           'a cutaway of the planet Mercury showing a large iron core, with faint magnetic field lines around it, '
           + STYLE + '.'),
    # ---- Venus
    custom('venus-probe', 'PROBE TO VENUS', '', 'golden yellow', 'white', 'white', 'deep black space',
           'golden yellow, cream, white and orange',
           'a small probe flying toward a large creamy-yellow cloud-covered planet Venus, ' + STYLE + '.'),
    custom('venus-flyby', 'VENUS FLYBY', '', 'golden yellow', 'white', 'white', 'deep black space',
           'golden yellow, cream, white and navy',
           'a small probe curving past the edge of a creamy-yellow cloud-covered Venus on a thin white arc, '
           + STYLE + '.'),
    custom('venus-structure-study', 'VENUS STUDIED', '', 'dark gold', 'white', 'white', 'deep black space',
           'dark gold, cream, white and orange',
           'a cutaway of the planet Venus showing thick yellow clouds above an orange rocky surface, ' + STYLE + '.'),
    custom('venus-orbit', 'VENUS ORBIT', '', 'golden yellow', 'gold', 'white', 'deep black space',
           'golden yellow, gold, white and cream',
           'a probe circling a creamy-yellow cloud-covered Venus on a full elliptical orbit line, ' + STYLE + '.'),
    flown('pioneer-venus-orbiter-flown', 'PIONEER VENUS', 'ORBITER', NASA,
          'a drum-shaped spacecraft with a tall antenna mast, circling a creamy-yellow cloud-covered Venus'),
    flown('venera-15-flown', 'VENERA 15', 'RADAR MAPPER', SOVIET,
          'a spacecraft with a large flat radar antenna and dish, circling Venus and sweeping a radar beam over '
          'its clouds'),
    custom('venus-radar-mapping', 'VENUS RADAR MAP', '', 'deep orange', 'gold', 'white', 'deep black space',
           'orange, gold, white and brown',
           'half of Venus shown with its clouds peeled away to reveal an orange-brown radar map of mountains and '
           'plains, ' + STYLE + '.'),
    custom('venus-structure-survey', 'VENUS SURVEYED', '', 'dark gold', 'gold', 'white', 'deep black space',
           'dark gold, white, cream and brown',
           'Venus with a grid of survey lines over its yellow clouds, circled by a probe, ' + STYLE + '.'),
    custom('venus-atmospheric-entry', 'INTO THE CLOUDS', '', 'golden yellow', 'gold', 'white',
           'deep black space', 'golden yellow, gold, white and orange',
           'four small cone-shaped probes falling into thick yellow clouds on glowing entry trails, ' + STYLE + '.'),
    custom('venus-atmosphere-study', 'VENUS ATMOSPHERE', '', 'amber', 'gold', 'white', 'deep black space',
           'amber, gold, white and yellow',
           'a probe hanging under a parachute deep inside swirling yellow clouds, ' + STYLE + '.'),
    custom('venus-lander', 'LANDER ON VENUS', '', 'burnt orange', 'gold', 'white', 'deep orange sky',
           'burnt orange, gold, white and brown',
           'the Venera lander: a heavily armored silver sphere wearing a wide flat disc-shaped collar around its '
           'middle like a hat brim, standing on a round ring-shaped landing base, on flat slabs of orange rock '
           'under a hazy orange sky, '
           + STYLE + '.'),
    custom('venus-surface-study', 'SURFACE OF VENUS', '', 'burnt orange', 'gold', 'white', 'deep orange sky',
           'burnt orange, gold, white and brown',
           'flat jagged slabs of rock on the orange surface of Venus under a hazy sky, seen through a camera '
           'lens, ' + STYLE + '.'),
    custom('venus-lander-and-carrier', 'VENUS LANDER', 'AND CARRIER', 'dark gold', 'gold', 'white',
           'deep black space', 'dark gold, white, orange and navy',
           'a carrier spacecraft passing above Venus while a small lander descends below it toward the clouds, '
           'with a radio link drawn between them, ' + STYLE + '.'),
    # ---- Mars
    custom('mars-probe', 'PROBE TO MARS', '', 'rust red', 'gold', 'white', 'deep black space',
           'rust red, gold, white and orange',
           'a small probe flying toward a large rust-red planet Mars with white polar caps, ' + STYLE + '.'),
    custom('mars-flyby', 'MARS FLYBY', '', 'rust red', 'silver', 'white', 'deep black space',
           'rust red, silver, white and orange',
           'a probe with four solar panels curving past the edge of a red planet Mars on a thin white arc, '
           + STYLE + '.'),
    custom('mars-photos', 'PICTURES OF MARS', '', 'dark red', 'gold', 'white', 'deep black space',
           'dark red, gold, white and orange',
           'a camera lens reflecting a red cratered Mars, with a strip of film frames curving around it, '
           + STYLE + '.'),
    custom('mars-surface-and-atmosphere-study', 'MARS STUDIED', '', 'rust red', 'gold', 'white',
           'deep black space', 'rust red, gold, white and pale orange',
           'the planet Mars with a thin hazy atmosphere glowing at its edge and a small probe passing by, '
           + STYLE + '.'),
    custom('mars-orbit', 'MARS ORBIT', '', 'rust red', 'gold', 'white', 'deep black space',
           'rust red, gold, white and orange',
           'a probe circling a red Mars on a full elliptical orbit line, with the two tiny moons nearby, '
           + STYLE + '.'),
    custom('mars-orbiter-science', 'MARS FROM ORBIT', '', 'dark red', 'gold', 'white', 'deep black space',
           'dark red, gold, white and orange',
           'an orbiter with solar panels circling Mars, with a huge volcano and a long canyon visible on the planet, '
           + STYLE + '.'),
    flown('mars-m69-flown', 'MARS 69', 'ORBITER', SOVIET,
          'a heavy cylindrical spacecraft with solar panels and a dish antenna, circling a red Mars'),
    custom('mars-mapping', 'MARS MAPPED', '', 'dark red', 'gold', 'white', 'deep black space',
           'dark red, gold, white and orange',
           'a red Mars covered with a fine grid of map lines, ' + STYLE + '.'),
    custom('mars-surface-and-atmosphere-survey', 'MARS SURVEYED', '', 'rust red', 'gold', 'white',
           'deep black space', 'rust red, gold, white and orange',
           'Mars with survey lines over its surface and swirling dust clouds, circled by an orbiter, ' + STYLE + '.'),
    custom('mars-orbiter-and-lander', 'MARS ORBITER', 'AND LANDER', 'deep red', 'gold', 'white',
           'deep black space', 'deep red, gold, white and orange',
           'an orbiter above Mars and a three-legged lander standing on the red rocky surface below, linked by a '
           'radio beam, ' + STYLE + '.'),
    custom('mars-soil-study', 'MARS SOIL', 'STUDIED', 'rust brown', 'gold', 'white', 'pale orange sky',
           'rust brown, gold, white and orange',
           'a robotic arm with a small scoop digging into red Martian soil under a pale orange sky, ' + STYLE + '.'),
    custom('mars-orbiter-lander-and-rover', 'MARS LANDER', 'AND ROVER', 'deep red', 'gold', 'white',
           'pale orange sky', 'deep red, gold, white and orange',
           'a round lander with open petals on the red Martian surface and a small box-shaped rover on two skis '
           'beside it, tied to it by a thin cable, ' + STYLE + '.'),
    custom('mars-soil-analysis', 'MARS SOIL', 'ANALYZED', 'rust brown', 'gold', 'white', 'pale orange sky',
           'rust brown, gold, white and orange',
           'a small glass laboratory flask filled with red Martian soil, standing in front of the planet Mars, '
           + STYLE + '.'),
    # ---- Jupiter and Saturn
    custom('jupiter-probe', 'PROBE TO JUPITER', '', 'burnt sienna', 'gold', 'white', 'deep black space',
           'burnt sienna, gold, cream and white',
           'a probe with a large white dish antenna flying toward a huge banded planet Jupiter with its great red '
           'spot, ' + STYLE + '.'),
    custom('jupiter-flyby', 'JUPITER FLYBY', '', 'burnt sienna', 'silver', 'white', 'deep black space',
           'burnt sienna, silver, cream and white',
           'a probe with a large dish antenna curving past banded Jupiter on a thin white arc, ' + STYLE + '.'),
    custom('jupiter-atmosphere-study', 'JUPITER ATMOSPHERE', '', 'tan brown', 'gold', 'white', 'deep black space',
           'tan, gold, cream and orange',
           'a close view of Jupiter\'s swirling cloud bands and the great red spot, ' + STYLE + '.'),
    custom('jupiter-photos', 'PICTURES OF JUPITER', '', 'burnt sienna', 'gold', 'white', 'deep black space',
           'burnt sienna, gold, cream and white',
           'a camera lens reflecting banded Jupiter, with a strip of film frames curving around it, ' + STYLE + '.'),
    custom('jupiter-moon-photos', 'MOONS OF JUPITER', '', 'deep brown', 'gold', 'white', 'deep black space',
           'brown, gold, cream and white',
           'banded Jupiter with four small moons lined up beside it, ' + STYLE + '.'),
    custom('saturn-probe', 'PROBE TO SATURN', '', 'pale gold', 'white', 'white', 'deep black space',
           'pale gold, cream, white and navy',
           'a probe with a large dish antenna flying toward Saturn with its wide flat rings, ' + STYLE + '.'),
    custom('saturn-flyby', 'SATURN FLYBY', '', 'pale gold', 'silver', 'white', 'deep black space',
           'pale gold, silver, cream and navy',
           'a probe curving past Saturn and its rings on a thin white arc, ' + STYLE + '.'),
    custom('saturn-structure-study', 'SATURN STUDIED', '', 'golden tan', 'gold', 'white', 'deep black space',
           'golden tan, gold, cream and white',
           'a close view of Saturn\'s rings showing many thin bands and gaps, ' + STYLE + '.'),
    # ---- Space planes
    flown('x-15-flight-test-flown', 'X-15', 'FLIGHT TEST', NASA,
          'the X-15: a black, stubby-winged rocket plane with a pointed nose and a wedge-shaped tail, dropping '
          'away from under the wing of a large silver bomber, with a short flame from its rocket engine'),
    flown('x-15-research-flight-1-flown', 'X-15', 'ALTITUDE FLIGHT', NASA,
          'the X-15: a black, stubby-winged rocket plane climbing steeply toward black space above the thin blue '
          'curve of the atmosphere'),
    flown('x-15-research-flight-2-flown', 'X-15', 'SPEED FLIGHT', NASA,
          'the X-15: a black, stubby-winged rocket plane streaking level and fast, glowing hot at its nose, with '
          'speed lines trailing behind it over a desert lakebed'),
    flown('mig-e-154-drop-test-flown', 'MIG E-154', 'DROP TEST', SOVIET,
          'a slender silver swept-wing rocket plane with a red star on its tail fin, falling away from under a '
          'large carrier aircraft'),
    # The E-154 is the Soviet X-15 (2026-10-17): its altitude flight keeps the suborbital patch (it goes to the edge
    # of space), and its speed flight's is still to be made.
    flown('mig-e-154-research-flight-1-flown', 'MIG E-154', 'SUBORBITAL FLIGHT', SOVIET,
          'a slender silver swept-wing rocket plane with a red star on its tail fin, at the top of a high arc '
          'above the curved Earth'),
    flown('mig-e-154-research-flight-2-flown', 'MIG E-154', 'SPEED FLIGHT', SOVIET,
          'a slender silver swept-wing rocket plane with a red star on its tail fin, streaking level and fast, '
          'glowing hot at its needle nose, with speed lines trailing behind it above the clouds'),
    # ---- Two-crew capsules
    uncrewed_suborbital('gemini-uncrewed-suborbital-flown', 'GEMINI', NASA, GEMINI),
    suborbital('gemini-suborbital-flown', 'GEMINI', NASA, GEMINI),
    uncrewed_orbital('gemini-uncrewed-orbital-flown', 'GEMINI', NASA, GEMINI),
    orbital('gemini-orbital-flown', 'GEMINI', NASA, GEMINI),
    spacewalk('gemini-spacewalk-flown', 'GEMINI', NASA, GEMINI),
    side('gemini-rendezvous-flown', 'GEMINI', 'RENDEZVOUS', NASA,
         f'two black Gemini capsules flying nose to nose a few meters apart above the curved blue Earth, {STYLE}.'),
    side('gemini-docking-flown', 'GEMINI', 'DOCKING', NASA,
         f'{GEMINI}, docked nose-first into the collar of a long slim cylindrical Agena target stage, above the '
         f'curved blue Earth, {STYLE}.'),
    reentry_test('gemini-lunar-reentry-test-flown', 'GEMINI', NASA, 'a black Gemini capsule'),
    side('gemini-transtage-test-flown', 'GEMINI', 'TRANSTAGE TEST', NASA,
         f'a black Gemini capsule docked to a cylindrical rocket stage firing its engine, above the curved Earth, '
         f'{STYLE}.'),
    side('gemini-lunar-propulsion-module-test-flown', 'LUNAR GEMINI', 'ENGINE TEST', NASA,
         f'{GEMINI_LUNAR}, firing its engine in Earth orbit, {STYLE}.'),
    uncrewed_orbital('gemini-direct-ascent-uncrewed-test-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    orbital('gemini-direct-ascent-crewed-test-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    uncrewed_suborbital('voskhod-uncrewed-suborbital-flown', 'VOSKHOD', SOVIET, VOSKHOD),
    suborbital('voskhod-suborbital-flown', 'VOSKHOD', SOVIET, VOSKHOD),
    uncrewed_orbital('voskhod-uncrewed-orbital-flown', 'VOSKHOD', SOVIET, VOSKHOD),
    orbital('voskhod-orbital-flown', 'VOSKHOD', SOVIET, VOSKHOD),
    side('voskhod-spacewalk-flown', 'VOSKHOD', 'SPACEWALK', SOVIET,
         f'{VOSKHOD}, with a tube-shaped airlock sticking out of its side and a tiny figure in a white spacesuit '
         f'floating beside it on a tether, above the curved blue Earth, {STYLE}.'),
    uncrewed_suborbital('soyuz-7k-l1-uncrewed-suborbital-flown', 'SOYUZ 7K-L1', SOVIET, SOYUZ_L1),
    suborbital('soyuz-7k-l1-suborbital-flown', 'SOYUZ 7K-L1', SOVIET, SOYUZ_L1),
    uncrewed_orbital('soyuz-7k-l1-uncrewed-orbital-flown', 'SOYUZ 7K-L1', SOVIET, SOYUZ_L1),
    orbital('soyuz-7k-l1-orbital-flown', 'SOYUZ 7K-L1', SOVIET, SOYUZ_L1),
    uncrewed_suborbital('soyuz-7k-l3-uncrewed-suborbital-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    suborbital('soyuz-7k-l3-suborbital-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    uncrewed_orbital('soyuz-7k-l3-uncrewed-orbital-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    orbital('soyuz-7k-l3-orbital-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    spacewalk('soyuz-7k-l3-spacewalk-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    side('lk-lander-uncrewed-test-flown', 'LK LANDER', 'UNCREWED TEST', SOVIET,
         f'{LK}, firing its engine while circling the Earth on an orbit line, {STYLE}.'),
    side('lk-lander-test-flown', 'LK LANDER', 'CREWED TEST', SOVIET,
         f'{LK}, firing its engine while circling the Earth, with a small helmet emblem shining above it, '
         f'{STYLE}.'),
    uncrewed_suborbital('lk-700-uncrewed-suborbital-flown', 'LK-700', SOVIET, LK700),
    suborbital('lk-700-suborbital-flown', 'LK-700', SOVIET, LK700),
    uncrewed_orbital('lk-700-uncrewed-orbital-flown', 'LK-700', SOVIET, LK700),
    orbital('lk-700-orbital-flown', 'LK-700', SOVIET, LK700),
    spacewalk('lk-700-spacewalk-flown', 'LK-700', SOVIET, LK700),
    side('langley-lander-test-flown', 'LANGLEY LANDER', 'ORBIT TEST', NASA,
         f'{LANGLEY}, flying free in Earth orbit next to a black Gemini capsule, {STYLE}.'),
    side('gemini-eor-service-module-test-flown', 'GEMINI EOR', 'MODULE TEST', NASA,
         f'{GEMINI_EOR}, circling the Earth on an orbit line, {STYLE}.'),
    # ---- Lunar expeditions
    # "UNCREWED LUNAR FLYBY" came out misspelled on nine variants here; this text renders cleanly.
    dict(uncrewed_flyby('circumlunar-gemini-transtage-flyby-flown', 'GEMINI', NASA,
                        'an empty black Gemini capsule pushed by a cylindrical rocket stage'), bottom='AROUND THE MOON'),
    flyby('circumlunar-gemini-flyby-flown', 'GEMINI', NASA, 'a black Gemini capsule pushed by a cylindrical '
          'rocket stage'),
    uncrewed_flyby('lunar-orbital-gemini-uncrewed-flyby-flown', 'LUNAR GEMINI', NASA, GEMINI_LUNAR),
    flyby('lunar-orbital-gemini-flyby-flown', 'LUNAR GEMINI', NASA, GEMINI_LUNAR),
    uncrewed_lunar_orbit('lunar-orbital-gemini-uncrewed-lunar-orbit-flown', 'LUNAR GEMINI', NASA, GEMINI_LUNAR),
    lunar_orbit('lunar-orbital-gemini-lunar-orbit-flown', 'LUNAR GEMINI', NASA, GEMINI_LUNAR),
    uncrewed_flyby('gemini-direct-ascent-uncrewed-flyby-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    flyby('gemini-direct-ascent-flyby-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    uncrewed_lunar_orbit('gemini-direct-ascent-uncrewed-lunar-orbit-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    lunar_orbit('gemini-direct-ascent-lunar-orbit-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    landing('gemini-direct-ascent-landing-flown', 'DIRECT ASCENT', NASA, GEMINI_DA),
    uncrewed_flyby('apollo-uncrewed-flyby-flown', 'APOLLO', NASA, APOLLO),
    flyby('apollo-flyby-flown', 'APOLLO', NASA, APOLLO),
    uncrewed_lunar_orbit('apollo-uncrewed-lunar-orbit-flown', 'APOLLO', NASA, APOLLO),
    lunar_orbit('apollo-lunar-orbit-flown', 'APOLLO', NASA, APOLLO),
    lander_test('apollo-lander-test-flown', 'APOLLO', NASA, APOLLO, LM),
    landing('apollo-landing-flown', 'APOLLO', NASA, LM),
    side('apollo-landing-with-rover-flown', 'APOLLO', 'LUNAR ROVER', NASA,
         f'a small open four-wheeled electric car with two astronauts in white spacesuits driving across the gray '
         f'lunar surface, with a gold-legged lunar module in the distance and the Earth in the black sky, {STYLE}.'),
    flyby('gemini-eor-flyby-flown', 'GEMINI EOR', NASA, GEMINI_EOR),
    lunar_orbit('gemini-eor-lunar-orbit-flown', 'GEMINI EOR', NASA, GEMINI_EOR),
    lander_test('gemini-eor-lander-test-flown', 'GEMINI EOR', NASA, 'a black Gemini capsule', LANGLEY),
    landing('gemini-eor-landing-flown', 'GEMINI EOR', NASA, LANGLEY),
    uncrewed_flyby('soyuz-7k-l1-uncrewed-flyby-flown', 'SOYUZ 7K-L1', SOVIET, SOYUZ_L1),
    flyby('soyuz-7k-l1-flyby-flown', 'SOYUZ 7K-L1', SOVIET, SOYUZ_L1),
    uncrewed_flyby('soyuz-7k-l3-uncrewed-flyby-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    flyby('soyuz-7k-l3-flyby-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    uncrewed_lunar_orbit('soyuz-7k-l3-uncrewed-lunar-orbit-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    lunar_orbit('soyuz-7k-l3-lunar-orbit-flown', 'SOYUZ 7K-LOK', SOVIET, SOYUZ_LOK),
    lander_test('soyuz-7k-l3-lander-test-flown', 'LK LANDER', SOVIET, SOYUZ_LOK, LK),
    landing('soyuz-7k-l3-landing-flown', 'LK LANDER', SOVIET, LK),
    uncrewed_flyby('lk-700-uncrewed-flyby-flown', 'LK-700', SOVIET, LK700),
    flyby('lk-700-flyby-flown', 'LK-700', SOVIET, LK700),
    uncrewed_lunar_orbit('lk-700-uncrewed-lunar-orbit-flown', 'LK-700', SOVIET, LK700),
    lunar_orbit('lk-700-lunar-orbit-flown', 'LK-700', SOVIET, LK700),
    landing('lk-700-landing-flown', 'LK-700', SOVIET, LK700),
    # ---- Three-crew capsules
    uncrewed_suborbital('apollo-uncrewed-suborbital-flown', 'APOLLO', NASA, APOLLO),
    suborbital('apollo-suborbital-flown', 'APOLLO', NASA, APOLLO),
    uncrewed_orbital('apollo-uncrewed-orbital-flown', 'APOLLO', NASA, APOLLO),
    orbital('apollo-orbital-flown', 'APOLLO', NASA, APOLLO),
    spacewalk('apollo-spacewalk-flown', 'APOLLO', NASA, APOLLO),
    side('apollo-uncrewed-lunar-module-test-flown', 'LUNAR MODULE', 'UNCREWED TEST', NASA,
         f'{LM}, firing its engine while circling the Earth on an orbit line, {STYLE}.'),
    side('apollo-lunar-module-test-flown', 'LUNAR MODULE', 'CREWED TEST', NASA,
         f'{LM}, flying apart from the silver Apollo command and service module above the curved Earth, {STYLE}.'),
    reentry_test('apollo-lunar-reentry-test-flown', 'APOLLO', NASA, 'a silver cone-shaped Apollo command module'),
    uncrewed_suborbital('soyuz-7k-ok-uncrewed-suborbital-flown', 'SOYUZ', SOVIET, SOYUZ_OK),
    suborbital('soyuz-7k-ok-suborbital-flown', 'SOYUZ', SOVIET, SOYUZ_OK),
    uncrewed_orbital('soyuz-7k-ok-uncrewed-orbital-flown', 'SOYUZ', SOVIET, SOYUZ_OK),
    orbital('soyuz-7k-ok-orbital-flown', 'SOYUZ', SOVIET, SOYUZ_OK),
    spacewalk('soyuz-7k-ok-spacewalk-flown', 'SOYUZ', SOVIET, SOYUZ_OK),
    # ---- Any Soyuz
    uncrewed_suborbital('soyuz-uncrewed-suborbital', 'SOYUZ', SOVIET, SOYUZ_OK),
    suborbital('soyuz-suborbital', 'SOYUZ', SOVIET, SOYUZ_OK),
    uncrewed_orbital('soyuz-uncrewed-orbital', 'SOYUZ', SOVIET, SOYUZ_OK),
    orbital('soyuz-orbital', 'SOYUZ', SOVIET, SOYUZ_OK),
    uncrewed_flyby('soyuz-uncrewed-lunar-flyby', 'SOYUZ', SOVIET, SOYUZ_L1),
    flyby('soyuz-lunar-flyby', 'SOYUZ', SOVIET, SOYUZ_L1),
    # ---- Milestones of the race
    custom('duration-level-3', 'A WEEK IN SPACE', '', 'deep teal', 'gold', 'white', 'deep navy-blue space',
           'teal, navy, gold and white',
           'a blue Earth wrapped in many thin orbit lines with a small capsule on the outermost one, and a row of '
           'seven small suns arching over the top, ' + STYLE + '.'),
    custom('duration-level-4', 'LONG FLIGHT', '', 'dark plum', 'gold', 'white', 'deep navy-blue space',
           'plum, navy, gold and white',
           'a small capsule on a long spiral of orbit lines around the Earth, with a stylized calendar page in the '
           'sky above, ' + STYLE + '.'),
    custom('duration-level-5', 'WEEKS IN SPACE', '', 'deep olive', 'gold', 'white', 'deep navy-blue space',
           'olive, navy, gold and white',
           'a small spacecraft with solar panels circling the Earth among many orbit lines, with rows of small '
           'suns and crescent moons arching overhead, ' + STYLE + '.'),
    custom('duration-level-6', 'LIVING IN SPACE', '', 'deep navy', 'gold', 'white', 'deep black space',
           'navy, gold, white and sky blue',
           'a spacecraft with solar panels and a small window lit warmly from inside, circling the Earth, '
           + STYLE + '.'),
    custom('spacewalk', 'SPACEWALK', '', 'deep navy', 'silver', 'white', 'deep black space',
           'navy, silver, white and sky blue',
           'a single figure in a white spacesuit floating freely above the curved blue Earth, attached to a '
           'capsule by a long golden tether, ' + STYLE + '.'),
    custom('crew-transfer', 'CREW TRANSFER', '', 'steel blue', 'gold', 'white', 'deep black space',
           'steel blue, gold, white and silver',
           'two docked spacecraft nose to nose above the Earth, with a small figure in a white spacesuit climbing '
           'along the outside from one to the other, ' + STYLE + '.'),
    custom('rendezvous', 'RENDEZVOUS', '', 'teal blue', 'silver', 'white', 'deep black space',
           'teal, silver, white and blue',
           'two small generic spacecraft meeting nose to nose above the curved Earth, their two orbit lines '
           'merging into one, ' + STYLE + '.'),
    custom('docking', 'DOCKING', '', 'royal blue', 'gold', 'white', 'deep black space',
           'royal blue, gold, white and silver',
           'two generic spacecraft joined nose to nose by a docking collar, forming one long craft above the '
           'Earth, ' + STYLE + '.'),
    custom('crewed-lunar-flyby', 'AROUND THE MOON', '', 'midnight blue', 'silver', 'white', 'deep black space',
           'midnight blue, silver, white and gray',
           'a small capsule on a figure-eight path looping around a large gray Moon and back to the Earth, with a '
           'small pilot helmet emblem above, ' + STYLE + '.'),
    custom('crewed-lunar-orbit', 'CREW IN LUNAR ORBIT', '', 'deep navy', 'gold', 'white', 'deep black space',
           'navy, gold, white and gray',
           'a blue-and-white Earth rising over the gray horizon of the Moon, seen past the edge of a capsule '
           'window, ' + STYLE + '.'),
    custom('crewed-lunar-landing', 'PEOPLE ON THE MOON', '', 'deep royal blue', 'gold', 'gold',
           'deep black space', 'royal blue, gold, white and gray',
           'a single boot print in fine gray lunar dust, with the blue Earth shining in the black sky above, '
           + STYLE + '.'),
    custom('lunar-samples-returned', 'MOON ROCKS HOME', '', 'deep maroon', 'gold', 'white',
           'deep navy-blue space', 'maroon, gold, white and gray',
           'a gray Moon rock resting on a small velvet cushion, with a capsule under parachutes and the Moon in '
           'the sky behind, ' + STYLE + '.'),
    custom('space-plane', 'SPACE PLANE', '', 'midnight blue', 'silver', 'white', 'deep black space',
           'midnight blue, silver, white and sky blue',
           'a sleek winged rocket plane climbing from the blue atmosphere into black space on a bright exhaust '
           'trail, ' + STYLE + '.'),
    custom('two-crew-spacecraft', 'CREW OF TWO', '', 'steel gray', 'gold', 'white', 'deep navy-blue space',
           'steel gray, navy, gold and white',
           'two pilot helmets side by side in front of a small capsule window, with the Earth beyond, '
           + STYLE + '.'),
    custom('three-crew-spacecraft', 'CREW OF THREE', '', 'deep navy', 'gold', 'white', 'deep navy-blue space',
           'navy, gold, white and sky blue',
           'three pilot helmets side by side in front of a capsule window, with the Earth beyond, ' + STYLE + '.'),
    custom('deep-space-probe', 'DEEP SPACE', '', 'deep purple', 'gold', 'white', 'deep black space',
           'deep purple, gold, white and orange',
           'a small probe with a large dish antenna heading away from a tiny blue Earth toward distant planets '
           'and the Sun, ' + STYLE + '.'),
]
