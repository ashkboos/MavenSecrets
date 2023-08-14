from common.config import Config
from database import Database
from common.packageId import PackageId


# This is a (very dirty) test script that shows that the find_best_match_tag()
# function finds the same tags matching the custom format as the sql query.
# Function from get_tags.py is slightly adapted but the filters are the same.
def main():
    config = Config()
    db = Database(
        config.DB_CONFIG["hostname"],
        config.DB_CONFIG["port"],
        config.DB_CONFIG["username"],
        config.DB_CONFIG["password"],
    )
    records = db.get_all_tags()
    query_records = db.get_all_matching_tags()
    count = 0
    print(f"Total records: {len(records)}")
    matching_tags = []
    for record in records:
        pkg = PackageId(record[0], record[1], record[2])
        if find_best_match_tag([record[3]], pkg):
            matching_tags.append([record[3]])
            count += 1

    print(f"Total matching: {count}")
    print(query_records.sort() == matching_tags.sort())


def find_best_match_tag(tags: list, pkg):
    artifactid, version = pkg.artifactid, pkg.version
    artifact_parts = artifactid.split("-")
    possible_tags: list[str] = [
        version,
        artifactid + "-" + version,
        "version-" + version,
        "v" + version,
        "v." + version,
        "release-" + version,
        "release-v" + version,
        "release_" + version,
        "release_v" + version,
        "release/" + version,
        "release/v" + version,
        "releases/" + version,
        "rel-" + version,
        "rel_" + version,
        "rel_v" + version,
        "rel/" + version,
        "rel/v" + version,
        "r" + version,
        "r." + version,
        "project-" + version,
        version + "-release",
        version + ".release",
        "v" + version + ".release",
        version + ".final",
        version + "-final",
        "v" + version + "-final",
        "tag-" + version,
        "tag" + version,
        artifact_parts[0] + "-" + version,
        artifact_parts[0] + "-v" + version,
    ]
    for i in range(5):
        if len(artifact_parts) > i:
            possible_tags.extend(
                [
                    artifact_parts[i] + "-" + version,
                    artifact_parts[i] + "-v" + version,
                ]
            )

    for i in range(5):
        if len(artifact_parts) > i:
            tag_str = "-".join(artifact_parts[: i + 1])
            possible_tags.append(tag_str + "-" + version)
            possible_tags.append(tag_str + "-v" + version)

    possible_tags = [tag.lower() for tag in possible_tags]
    if tags[0]:
        filtered = list(filter(lambda tag: tag.lower() in possible_tags, tags))
        if len(filtered) > 0:
            return True
    else:
        return False


main()
