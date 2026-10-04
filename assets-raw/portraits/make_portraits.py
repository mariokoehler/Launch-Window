"""Generates personnel portraits with the user's ComfyUI portrait workflow (Qwen Image 2.1).

Each portrait gets a prompt built from attributes spread deliberately across the batch: role,
sex, ethnicity, age group, clothing, hairstyle and small extras. The choices and the seed come
from the portrait's archetype and number, so a rerun produces the same prompts and seeds.

Usage (ComfyUI running locally):
    python make_portraits.py                  # 100 NASA and 100 Soviet portraits
    python make_portraits.py --count 10 --only soviet
    python make_portraits.py --force          # regenerate files that already exist

Writes <archetype>/<archetype>_NNN.png next to this script and manifest.json, which records
each portrait's attributes, prompt and seed. ComfyUI also keeps its own copy in its output
folder (named lw_portrait_<archetype>_NNN_*.png); this script never deletes those.
"""
import argparse
import json
import os
import random
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
WORKFLOW = os.path.join(HERE, 'portraits_workflow_api.json')
MANIFEST = os.path.join(HERE, 'manifest.json')

ROLES = ['engineer'] * 18 + ['flight controller'] * 16 + ['astronaut'] * 16

AGES = {
    'engineer': ['late twenties', 'thirties', 'early forties', 'late forties', 'fifties', 'early sixties'],
    'flight controller': ['late twenties', 'early thirties', 'late thirties', 'early forties', 'late forties',
                          'fifties'],
    'astronaut': ['late twenties', 'early thirties', 'mid thirties', 'late thirties', 'early forties'],
}

ETHNICITIES = {
    'nasa': ['white American', 'white American of Irish descent', 'white American of Italian descent',
             'white American of German descent', 'white American of Polish descent', 'Black American',
             'Black American', 'Mexican-American', 'Puerto Rican American', 'Japanese-American',
             'Chinese-American', 'Filipino-American', 'Native American (Navajo)', 'Korean-American',
             'white American of Scandinavian descent', 'Jewish American', 'Greek-American'],
    'soviet': ['Russian', 'Russian', 'Russian', 'Ukrainian', 'Belarusian', 'Georgian', 'Armenian', 'Kazakh',
               'Uzbek', 'Volga Tatar', 'Azerbaijani', 'Buryat', 'Estonian', 'Latvian', 'Lithuanian',
               'Soviet Jewish', 'Chuvash'],
}

CLOTHING = {
    ('nasa', 'engineer', 'male'): [
        'a dark two-button suit, white shirt and narrow dark tie',
        'a short-sleeved white dress shirt with a narrow tie and a pocket protector holding pens',
        'a white lab coat over a shirt and tie',
        'a gray cardigan over a white shirt and knit tie',
        'a light tweed sport coat over a plain shirt with an open collar',
        'a short-sleeved checked shirt buttoned to the collar with a clip-on tie'],
    ('nasa', 'engineer', 'female'): [
        'a fitted wool skirt suit jacket over a white blouse',
        'a white lab coat over a blouse with a Peter Pan collar',
        'a twin-set cardigan and sweater with a simple pearl necklace',
        'a high-necked blouse with a small brooch',
        'a dark sheath dress with a modest round collar'],
    ('nasa', 'flight controller', 'male'): [
        'a short-sleeved white shirt, narrow dark tie and a headset hanging around his neck',
        'a dark suit with a thin tie and a tie clip',
        'a short-sleeved striped shirt with a narrow tie',
        'a plain vest over a white shirt and narrow tie',
        'a white shirt with rolled-up sleeves and a loosened narrow tie'],
    ('nasa', 'flight controller', 'female'): [
        'a crisp white blouse with a headset around her neck',
        'a dark cardigan over a collared blouse',
        'a tailored jacket over a blouse with a narrow bow at the collar',
        'a simple shirtwaist dress with a narrow belt'],
    ('nasa', 'astronaut', 'male'): [
        'a silver pressure suit with the helmet off and the neck ring visible',
        'a light-colored flight suit with a zippered front and name tag',
        'a US Air Force officer uniform with wings insignia',
        'a US Navy officer uniform jacket with wings insignia',
        'a dark suit and narrow tie, as in an official astronaut portrait',
        'a leather flight jacket over a flight suit'],
    ('nasa', 'astronaut', 'female'): [
        'a light-colored flight suit with a zippered front and name tag',
        'a silver pressure suit with the helmet off',
        'a tailored blazer over a blouse, as in an official portrait',
        'a flight jacket over a collared shirt'],
    ('soviet', 'engineer', 'male'): [
        'a modest Soviet-era suit jacket with a white shirt and narrow tie',
        'a dark wool suit with wide lapels and a plain tie',
        'a knitted sweater over a collared shirt',
        'a gray work jacket over a shirt buttoned to the collar',
        'a white lab coat over a shirt and tie',
        'a turtleneck sweater under a plain jacket'],
    ('soviet', 'engineer', 'female'): [
        'a simple dark dress with a white collar',
        'a knitted cardigan over a plain blouse',
        'a white lab coat over a blouse',
        'a plain suit jacket over a blouse with a small brooch',
        'a high-necked wool sweater'],
    ('soviet', 'flight controller', 'male'): [
        'a Soviet Army officer tunic with shoulder boards',
        'a dark suit jacket with a white shirt and narrow tie and a headset around his neck',
        'a knitted vest over a white shirt and tie',
        'a plain gray suit with a dark tie'],
    ('soviet', 'flight controller', 'female'): [
        'a plain blouse with a headset around her neck',
        'a dark cardigan over a white blouse',
        'a simple dress with a narrow collar',
        'a uniform-style jacket over a white blouse'],
    ('soviet', 'astronaut', 'male'): [
        'an SK-1 pressure suit with the helmet off and the neck ring visible',
        'a Soviet Air Force officer uniform with shoulder boards',
        'a leather flight jacket over a flight suit',
        'a dark civilian suit with a medal on the lapel, as in an official portrait',
        'a light-colored training flight suit'],
    ('soviet', 'astronaut', 'female'): [
        'a pressure suit with the helmet off and the neck ring visible',
        'a Soviet Air Force uniform jacket',
        'a plain civilian suit jacket with a medal on the lapel',
        'a light-colored training flight suit'],
}

HAIR = {
    'male': {
        'young': ['a short crew cut', 'short hair neatly side-parted', 'short hair combed back with pomade',
                  'a flat-top haircut', 'short wavy hair', 'closely cropped curly hair'],
        'old': ['receding hair combed back', 'thinning gray hair', 'short gray hair side-parted',
                'mostly bald with short gray sides', 'salt-and-pepper hair combed back', 'short gray crew cut'],
    },
    'female': {
        'young': ['a short bob', 'hair in a neat bun', 'shoulder-length hair with curled ends',
                  'a short pixie cut', 'hair pinned up in a French twist', 'short curls'],
        'old': ['short graying curls', 'gray hair in a low bun', 'a short practical haircut with gray streaks',
                'graying hair pinned back', 'a short set wave hairstyle'],
    },
}

EXPRESSIONS = ['a serious, composed expression', 'a faint, reserved smile', 'a calm, attentive expression',
               'a stern, focused expression', 'a slightly tired but determined expression']

STYLE = {
    'nasa': ('Authentic early-1960s American government personnel photograph, black-and-white archival '
             'photography, slightly soft focus, subtle film grain, plain neutral gray studio background, even '
             'frontal illumination, restrained contrast, realistic facial features and skin texture. Formal '
             'composition with a straight-on pose, as for a NASA badge, a personnel file or an official '
             'portrait. Slight imperfections from period photographic processing are acceptable. No modern '
             'photography, no color.'),
    'soviet': ('Authentic early-1960s Soviet personnel dossier photograph, understated black-and-white archival '
               'photography, slightly soft focus, subtle photographic film grain, plain neutral gray studio '
               'background, even frontal illumination, restrained contrast, realistic facial features and skin '
               'texture. Formal, somewhat austere composition with a stiff official pose. The photograph should '
               'feel like a genuine archival image from a Soviet design bureau or space program organization, '
               'suitable for an official personnel file. Slight imperfections from period photographic '
               'processing are acceptable. No Western corporate aesthetic, no modern photography, no color.'),
}

# A short appearance note for ethnicities the model otherwise renders as European.
APPEARANCE = {
    'Kazakh': 'Central Asian, with East Asian facial features',
    'Uzbek': 'Central Asian, with dark hair and a darker complexion',
    'Buryat': 'Mongolic, with East Asian facial features',
    'Volga Tatar': 'with dark hair and slightly Central Asian features',
    'Chuvash': 'with dark hair and slightly East Asian features',
    'Georgian': 'from the Caucasus, with dark hair and an olive complexion',
    'Armenian': 'from the Caucasus, with dark hair and an olive complexion',
    'Azerbaijani': 'from the Caucasus, with dark hair and an olive complexion',
    'Black American': 'with dark brown skin',
    'Mexican-American': 'with a light brown complexion and dark hair',
    'Puerto Rican American': 'with a light brown complexion and dark hair',
    'Japanese-American': 'with East Asian facial features',
    'Chinese-American': 'with East Asian facial features',
    'Korean-American': 'with East Asian facial features',
    'Filipino-American': 'with Southeast Asian facial features and a brown complexion',
    'Native American (Navajo)': 'with Native American facial features and a brown complexion',
}

ORGS = {
    'nasa': {'engineer': 'an engineer at NASA or one of its contractors',
             'flight controller': 'a flight controller at NASA mission control',
             'astronaut': 'a NASA astronaut'},
    'soviet': {'engineer': 'an engineer at a Soviet aerospace design bureau',
               'flight controller': 'a flight controller at a Soviet mission control center',
               'astronaut': 'a Soviet cosmonaut'},
}


def spread(values, count, rng):
    """Repeats values to the count, then shuffles, so every value appears about evenly."""
    out = (values * (count // len(values) + 1))[:count]
    rng.shuffle(out)
    return out


BLOCK = 50


def portraits(archetype, count):
    """Returns the attributes, prompt and seed of every portrait of an archetype, in blocks of
    BLOCK: each block has its own random sequence, so adding blocks never changes earlier ones."""
    result = []
    for block in range((count + BLOCK - 1) // BLOCK):
        size = min(BLOCK, count - block * BLOCK)
        result.extend(portrait_block(archetype, block, size))
    return result


def portrait_block(archetype, block, count):
    """Returns one block of portraits; block 0 keeps the first batch's sequence."""
    rng = random.Random('launch-window-portraits-' + archetype + ('' if block == 0 else f'-{block}'))
    roles = spread(ROLES, count, rng)
    sexes = spread(['male', 'male', 'male', 'female', 'female'], count, rng)
    ethnicities = spread(ETHNICITIES[archetype], count, rng)
    result = []
    for i in range(count):
        role, sex = roles[i], sexes[i]
        age = rng.choice(AGES[role])
        old = age in ('fifties', 'early sixties', 'late forties') and rng.random() < 0.8
        clothing = rng.choice(CLOTHING[(archetype, role, sex)])
        hair = rng.choice(HAIR[sex]['old' if old else 'young'])
        extras = []
        if rng.random() < 0.28:
            extras.append(rng.choice(['horn-rimmed glasses', 'thin wire-rimmed glasses', 'browline glasses']))
        if sex == 'male' and rng.random() < (0.22 if archetype == 'soviet' else 0.12):
            extras.append(rng.choice(['a neatly trimmed mustache', 'a thin mustache']))
        expression = rng.choice(EXPRESSIONS)
        person = 'man' if sex == 'male' else 'woman'
        ethnicity = ethnicities[i]
        looks = f' ({APPEARANCE[ethnicity]})' if ethnicity in APPEARANCE else ''
        subject = (f'A formal head-and-shoulders portrait photograph of a {ethnicity} {person}{looks} in '
                   f'{possessive(sex)} {age}, {ORGS[archetype][role]} in the early 1960s. The face is large in the '
                   f'frame, the shoulders cropped at the bottom edge, facing the camera directly. '
                   f'{pronoun(sex)} has {expression} and {hair}'
                   + (', and wears ' + ' and '.join(extras) if extras else '') + '. '
                   f'{pronoun(sex)} wears {clothing}.')
        prompt = 'Subject\n\n' + subject + '\n\nStyle\n\n' + STYLE[archetype]
        number = block * BLOCK + i + 1
        result.append({
            'file': f'{archetype}/{archetype}_{number:03d}.png', 'archetype': archetype, 'number': number,
            'role': role, 'sex': sex, 'ethnicity': ethnicities[i], 'age': age, 'clothing': clothing, 'hair': hair,
            'extras': extras, 'seed': rng.randrange(2 ** 48), 'prompt': prompt,
        })
    return result


def pronoun(sex):
    return 'He' if sex == 'male' else 'She'


def possessive(sex):
    return 'his' if sex == 'male' else 'her'


def generate(host, entry, workflow):
    """Queues one portrait, waits for it, and saves it at the entry's file."""
    graph = json.loads(json.dumps(workflow))
    graph.pop('_comment', None)
    graph['474']['inputs']['prompt'] = entry['prompt']
    graph['458']['inputs']['seed'] = entry['seed']
    graph['461']['inputs']['filename_prefix'] = 'lw_portrait_' + entry['archetype'] + f"_{entry['number']:03d}"
    request = urllib.request.Request(host + '/prompt', data=json.dumps({'prompt': graph}).encode(),
                                     headers={'Content-Type': 'application/json'})
    prompt_id = json.load(urllib.request.urlopen(request))['prompt_id']
    deadline = time.time() + 600
    while time.time() < deadline:
        history = json.load(urllib.request.urlopen(host + '/history/' + prompt_id))
        if prompt_id in history:
            status = history[prompt_id].get('status', {})
            if status.get('status_str') == 'error':
                raise RuntimeError(json.dumps(status)[:1000])
            for output in history[prompt_id]['outputs'].values():
                for image in output.get('images', []):
                    query = urllib.parse.urlencode({'filename': image['filename'], 'subfolder': image['subfolder'],
                                                    'type': image['type']})
                    data = urllib.request.urlopen(host + '/view?' + query).read()
                    path = os.path.join(HERE, entry['file'])
                    os.makedirs(os.path.dirname(path), exist_ok=True)
                    with open(path, 'wb') as out:
                        out.write(data)
                    return
            raise RuntimeError('no image in the output')
        time.sleep(1)
    raise RuntimeError('timed out')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='http://127.0.0.1:8188')
    parser.add_argument('--count', type=int, default=100)
    parser.add_argument('--only', choices=['nasa', 'soviet'])
    parser.add_argument('--force', action='store_true')
    args = parser.parse_args()
    with open(WORKFLOW, encoding='utf-8') as source:
        workflow = json.load(source)
    archetypes = [args.only] if args.only else ['nasa', 'soviet']
    manifest = []
    for archetype in archetypes:
        manifest.extend(portraits(archetype, args.count))
    if args.only and os.path.exists(MANIFEST):
        with open(MANIFEST, encoding='utf-8') as source:
            manifest = [e for e in json.load(source) if e['archetype'] != args.only] + manifest
    with open(MANIFEST, 'w', encoding='utf-8', newline='\n') as out:
        json.dump(manifest, out, indent=2, ensure_ascii=False)
        out.write('\n')
    todo = [e for e in manifest if e['archetype'] in archetypes
            and (args.force or not os.path.exists(os.path.join(HERE, e['file'])))]
    start = time.time()
    for n, entry in enumerate(todo, 1):
        generate(args.host, entry, workflow)
        print(f"{n}/{len(todo)} {entry['file']} ({entry['role']}, {entry['sex']}, {entry['ethnicity']}, "
              f"{entry['age']}) {round(time.time() - start)} s", flush=True)
    print('done')


if __name__ == '__main__':
    sys.exit(main())
