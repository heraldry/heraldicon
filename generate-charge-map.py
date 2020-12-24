#!/usr/bin/env python3
import os
import re
from glob import glob

import edn_format
from edn_format.edn_lex import Keyword as K, Symbol

lookup = {}
placeholder_colours_map = {}


def group_by_type(coll):
    groups = {}
    for child_data in coll:
        type = child_data[K("type")]
        type = K(type.name + "s")
        if type not in groups:
            groups[type] = {}

        groups[type][child_data[K("key")]] = child_data

    return {
        k: dict(sorted(v.items(), key=lambda x: x[0].name))
        for k, v in sorted(groups.items(), key=lambda x: x[0].name)
    }


def get_supported_colours(data):
    result = set()
    for colour, key in placeholder_colours_map.items():
        if colour in data:
            result.add(key)

    return list(sorted(result, key=lambda x: x.name))


def remove_duplicate_slugs(name, nodes):
    for node in nodes:
        if node[K("type")] in [K("attitude"), K("charge")]:
            name = name.replace(node[K("key")].name, "")

    name = re.sub(r"--+", "-", name)
    return name


def traverse(directory, data_path, nodes):
    global lookup

    dirs = glob(os.path.join(directory, "*/"))
    files = glob(os.path.join(directory, "*.edn"))
    dirname = os.path.basename(directory.rstrip("/"))
    print(f"processing {dirname}...")
    if dirname == "charges":
        type = "root"
        name = "root"
    else:
        type, name = dirname.split("-", 1)

    key = K(name)
    data = {
        K("type"): K(type),
        K("key"): key,
        K("name"): name.replace("-", " "),
    }

    if type == "charge":
        lookup[key] = data_path[1:]

    if dirs:

        def dir_data(d):
            return traverse(d, data_path + [key], nodes + [data])

        data.update(group_by_type(map(dir_data, dirs)))

    if files:

        def file_data(f, parent_data):
            filename = os.path.basename(f)
            variant_key = filename.rsplit(".", 1)[0]
            variant_key = remove_duplicate_slugs(variant_key, nodes + [parent_data])
            if variant_key == "variant-default":
                variant_key = "default"

            name = variant_key.replace("-", " ").replace("-", " ")

            data = {
                K("key"): K(variant_key),
                K("type"): K("variant"),
                K("name"): name,
                K("path"): os.path.join(directory, filename),
            }

            supported_colours = get_supported_colours(open(f).read())
            if supported_colours:
                data[K("supported-tinctures")] = supported_colours

            return data

        data.update(group_by_type(map(lambda f: file_data(f, data), files)))

    return data


def read_config():
    global placeholder_colours_map
    config = edn_format.loads("[" + open("src/or/coad/config.cljs").read() + "]")
    for form in config:
        if len(form) > 1 and form[0:2] == (
            Symbol("def"),
            Symbol("placeholder-colours"),
        ):
            placeholder_colours_map = dict(map(lambda v: [v[1], v[0]], form[2].items()))


if __name__ == "__main__":
    read_config()

    os.chdir("main")
    data = traverse("data/charges", [], [])
    data[K("lookup")] = lookup
    edn = edn_format.dumps(data, indent=4)
    open("data/charge-map.edn", "w").write(edn)
