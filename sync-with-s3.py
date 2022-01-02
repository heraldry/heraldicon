#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import subprocess
from functools import partial


def md5sum(filename):
    with open(filename, mode="rb") as f:
        d = hashlib.md5()
        for buf in iter(partial(f.read, 128), b""):
            d.update(buf)
    return d.hexdigest()


def sync(directory, bucket):
    with subprocess.Popen(
        [
            "aws",
            "--profile",
            "heraldry-serverless",
            "s3api",
            "list-objects-v2",
            "--bucket",
            bucket,
        ],
        stdout=subprocess.PIPE,
    ) as process:
        data = json.load(process.stdout)
        etag_map = {d["Key"]: d["ETag"].strip('"') for d in data["Contents"]}

    os.chdir(directory)
    for root, _, files in os.walk("."):
        relative_root = root
        if relative_root.startswith("./"):
            relative_root = relative_root[2:]

        for filename in files:
            actual_filename = root + "/" + filename
            key = relative_root + "/" + filename
            if key.startswith("./"):
                key = key[2:]
            md5_hash = md5sum(actual_filename)
            etag = etag_map.get(key)
            if etag != md5_hash:
                with subprocess.Popen(
                    [
                        "aws",
                        "--profile",
                        "heraldry-serverless",
                        "s3",
                        "cp",
                        "--acl",
                        "public-read",
                        actual_filename,
                        f"s3://{bucket}/{key}",
                    ]
                ) as process:
                    process.wait()
                    assert process.returncode == 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("directory")
    parser.add_argument("bucket")

    args = parser.parse_args()

    sync(args.directory, args.bucket)
