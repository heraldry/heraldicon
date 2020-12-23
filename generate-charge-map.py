#!/usr/bin/env python3
import os
from glob import glob

import edn_format
from edn_format.edn_lex import Keyword as K

lookup = {}


def group_by_type(coll):
    groups = {}
    for child_data in coll:
        type = child_data[K("type")]
        type = K(type.name + "s")
        if type not in groups:
            groups[type] = {}

        groups[type][child_data[K("key")]] = child_data

    return groups


def traverse(directory, data_path):
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
        K("name"): name,
    }

    if type == "charge":
        lookup[key] = data_path[1:]

    if dirs:

        def dir_data(d):
            return traverse(d, data_path + [key])

        data.update(group_by_type(map(dir_data, dirs)))

    if files:

        def file_data(f):
            filename = os.path.basename(f)
            name = filename.rsplit(".", 1)[0]
            return {
                K("key"): K(name),
                K("type"): K("variant"),
                K("name"): name,
                K("path"): os.path.join(directory, filename),
            }

        data.update(group_by_type(map(file_data, files)))

    return data


if __name__ == "__main__":
    os.chdir("main")
    data = traverse("data/charges", [])
    data[K("lookup")] = lookup
    edn = edn_format.dumps(data, indent=4)
    open("data/charge-map.edn", "w").write(edn)
