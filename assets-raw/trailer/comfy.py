"""ComfyUI over HTTP for the trailer's generators: upload an image, queue a graph, wait, fetch the outputs.

Nodes are found by class and title (the user titles every input node in the workflows), never by id: the
ids of a flattened subgraph change when a workflow is re-exported.
"""
import io
import json
import time
import urllib.parse
import urllib.request
import uuid

HOST = 'http://127.0.0.1:8188'


def load(path):
    with open(path, encoding='utf-8') as source:
        graph = json.load(source)
    graph.pop('_comment', None)
    return graph


def node(graph, class_type, title=None):
    """The one node of this class (and title, if given); fails on none or several."""
    found = [k for k, v in graph.items() if v.get('class_type') == class_type
             and (title is None or v.get('_meta', {}).get('title') == title)]
    if len(found) != 1:
        raise KeyError(f'{len(found)} nodes {class_type} {title!r}')
    return found[0]


def remove(graph, node_id):
    """Removes a node and every input that links to it."""
    graph.pop(node_id)
    for v in graph.values():
        for name in [n for n, value in v.get('inputs', {}).items()
                     if isinstance(value, list) and len(value) == 2 and value[0] == node_id]:
            del v['inputs'][name]


def upload(image, name, host=HOST):
    """Uploads a PIL image to ComfyUI's input folder; returns the name to load it by."""
    data = io.BytesIO()
    image.save(data, 'PNG')
    boundary = uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="{name}"\r\n'
            f'Content-Type: image/png\r\n\r\n').encode() + data.getvalue() + \
           (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="overwrite"\r\n\r\ntrue'
            f'\r\n--{boundary}--\r\n').encode()
    request = urllib.request.Request(host + '/upload/image', data=body,
                                     headers={'Content-Type': f'multipart/form-data; boundary={boundary}'})
    answer = json.load(urllib.request.urlopen(request))
    return (answer['subfolder'] + '/' if answer.get('subfolder') else '') + answer['name']


def queue(graph, host=HOST):
    """Queues a graph; returns its prompt id."""
    request = urllib.request.Request(host + '/prompt', data=json.dumps({'prompt': graph}).encode(),
                                     headers={'Content-Type': 'application/json'})
    answer = json.load(urllib.request.urlopen(request))
    if answer.get('node_errors'):
        raise RuntimeError(json.dumps(answer['node_errors'])[:2000])
    return answer['prompt_id']


def known(prompt_id, host=HOST):
    """True if ComfyUI still knows this prompt (queued, running or in its history)."""
    if prompt_id in json.load(urllib.request.urlopen(host + '/history/' + prompt_id)):
        return True
    q = json.load(urllib.request.urlopen(host + '/queue'))
    return any(item[1] == prompt_id for item in q['queue_running'] + q['queue_pending'])


def run(graph, timeout=3600, host=HOST):
    """Queues a graph, waits for it, and returns the bytes of every file it wrote (images or videos)."""
    return wait(queue(graph, host), timeout, host)


def wait(prompt_id, timeout=3600, host=HOST):
    """Waits for a queued prompt and returns the bytes of every file it wrote."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        history = json.load(urllib.request.urlopen(host + '/history/' + prompt_id))
        if prompt_id in history:
            status = history[prompt_id].get('status', {})
            if status.get('status_str') == 'error':
                raise RuntimeError(json.dumps(status)[:2000])
            files = []
            for output in history[prompt_id]['outputs'].values():
                for kind in ('images', 'videos', 'gifs'):
                    for item in output.get(kind, []):
                        if item.get('type') != 'output':
                            continue
                        query = urllib.parse.urlencode({'filename': item['filename'],
                                                        'subfolder': item.get('subfolder', ''),
                                                        'type': item['type']})
                        files.append((item['filename'], urllib.request.urlopen(host + '/view?' + query).read()))
            if not files:
                raise RuntimeError('no file in the output')
            return files
        time.sleep(2)
    raise RuntimeError('timed out')
