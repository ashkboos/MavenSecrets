import collections

import matplotlib.pyplot as plt
import psycopg2

from pyscripts.database import Database


def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    conn = db.connect()
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)

    # Packaging type analysis
    # packaging_analysis(cur)

    # Checksum analysis
    checksum_analysis(cur)

    # Qualifier analysis
    # qualifier_analysis(cur)

    # Close the cursor and connection
    cur.close()
    conn.close()


def packaging_analysis(cur):
    # Individual frequency of each packaging type from pom
    print_plot_packaging_pom(cur)

    # Individual frequency of each packaging type from repo
    print_frequency_from_repo(cur)

    # Frequency of packages with frequency of packaging types
    print_frequency_of_packages_with_frequency_packaging_types(cur)

    # Frequency of pairs of difference in packaging type from pom and index
    print_difference_with_individual_freq(cur)

    # Number of different packages where index != repo
    print_frequency_index_packaging_different_repo(cur)


def print_plot_packaging_pom(cur):
    cur.execute('SELECT packagingtypefrompom, COUNT(*) FROM packages WHERE packagingtypefrompom IS NOT NULL GROUP BY '
                'packagingtypefrompom ORDER BY COUNT(*) DESC')
    results = cur.fetchall()
    # Create a dictionary to store the "packagingType" values and their counts
    packaging_type_counts = {}
    distinct_packaging_types = set()

    for row in results:
        packaging_type = row[0]
        count = row[1]
        packaging_type_counts[packaging_type] = count
        distinct_packaging_types.add(packaging_type)

    unique_packaging_type_count = len(distinct_packaging_types)

    print('PACKAGING TYPE FROM POM')
    print()

    for packaging_type, count in packaging_type_counts.items():
        print(f'Packaging type: {packaging_type}, Count: {count}')
    print()

    print(f'Total number of unique packaging types in pom: {unique_packaging_type_count}')
    print('---x----')
    print()


def print_frequency_from_repo(cur):
    cur.execute("SELECT allpackagingtype FROM packages")

    all_packagingtype_list = [row['allpackagingtype'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word(all_packagingtype_list)

    unique_words_count = len(sorted_frequencies)

    print('PACKAGING TYPE FROM THE REPO')
    print()

    # Print the word frequencies
    for word, frequency in sorted_frequencies:
        print(f'Packaging type: {word} - Frequency: {frequency}')
    print()

    print(f'Number of unique packaging types from the repo: {unique_words_count}')
    print('---x----')
    print()


def print_frequency_of_packages_with_frequency_packaging_types(cur):
    # Execute a query to fetch all values from the 'allpackagingtype' column in the table
    cur.execute("SELECT allpackagingtype FROM packages")

    # Create a dictionary to store the frequency of packaging types for each row
    row_frequency = collections.defaultdict(int)

    # Iterate over the rows and count the distinct packaging types for each row
    for row in cur.fetchall():
        allpackagingtype = row['allpackagingtype']
        if allpackagingtype is not None:
            packaging_types = set(allpackagingtype.strip('[]').split(','))
            num_packaging_types = len(packaging_types)
            row_frequency[num_packaging_types] += 1

    print('FREQUENCY OF PACKAGES WITH DIFFERENT PACKAGING TYPES')
    print()

    # Print the frequency of rows with each distinct count of packaging types
    for num_packaging_types, frequency in row_frequency.items():
        print(f'Frequency of packages with {num_packaging_types} packaging types: {frequency}')
    print('---x----')
    print()


def print_difference_with_individual_freq(cur):
    cur.execute('SELECT packagingtypefrompom, packagingtypefromrepo, COUNT(*) FROM packages WHERE '
                'packagingtypefrompom != packagingtypefromrepo GROUP BY '
                'packagingtypefrompom, packagingtypefromrepo')

    results = cur.fetchall()

    print('DIFFERENCE IN PACKAGING TYPE IN POM AND INDEX')
    print()

    total_frequency = 0
    for row in results:
        packagingtypefrompom = row[0]
        packagingtypefromrepo = row[1]
        frequency = row[2]
        total_frequency += frequency
        print(f'Packaging type from POM: {packagingtypefrompom}, Packaging type from INDEX: {packagingtypefromrepo}, '
              f'Frequency: {frequency}')
    print()

    print(f'Total Frequency of differences: {total_frequency}')
    print('---x----')
    print()


def print_frequency_index_packaging_different_repo(cur):
    # Execute a query to fetch the values from the 'allpackagingtype' and 'packagingtypefromrepo' columns
    cur.execute("SELECT allpackagingtype, packagingtypefromrepo FROM packages")

    count = 0

    # Iterate over the rows and check if the values are the same
    for row in cur.fetchall():
        allpackagingtype = row['allpackagingtype']
        packagingtypefromindex = row['packagingtypefromrepo']

        if allpackagingtype is not None:
            # Remove the square brackets and split the 'allpackagingtype' string into a list
            allpackagingtype_list = allpackagingtype.strip('[]').split(',')

            # Remove any leading or trailing whitespaces from each value in the 'allpackagingtype_list'
            allpackagingtype_list = [val.strip() for val in allpackagingtype_list]

            # Check if 'packagingtypefromindex' does not match all the values in 'allpackagingtype_list'
            if set(allpackagingtype_list) != {packagingtypefromindex}:
                count += 1

    print(f'Number of packages where the packaging type in the index is not same as packaging type(s) on repo: {count}')
    print('----x----')
    print()


def qualifier_analysis(cur):
    # Execute a query to fetch all values from the 'allqualifiers' column in the 'packages' table
    cur.execute("SELECT allqualifiers FROM packages")

    # Fetch all the values and store them into the 'all_qualifiers_list' list
    all_qualifiers_list = [row['allqualifiers'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word(all_qualifiers_list)

    unique_words_count = len(sorted_frequencies)

    print('QUALIFIER')
    print()

    # Print the word frequencies
    for word, frequency in sorted_frequencies:
        print(f'Qualifier: {word} - Frequency: {frequency}')
    print()

    print(f'Number of unique qualifiers: {unique_words_count}')
    print('---x----')
    print()


def print_frequency_of_checksums_over_years(cur):
    cur.execute("""
    SELECT EXTRACT(YEAR FROM pl.lastmodified) AS year, p.allchecksum
    FROM packages p
    INNER JOIN package_list pl ON p.groupid = pl.groupid AND p.artifactid = pl.artifactid AND p.version = pl.version
    """)

    # Fetch all the rows from the result
    rows = cur.fetchall()

    # Extract year and checksums
    year_checksums = {}
    for row in rows:
        year = row[0]
        checksums = row[1]

        if checksums is not None:
            # Remove leading/trailing whitespaces and split the string into words
            checksum_list = checksums.strip('[]').split(',')
            # Strip each checksum and remove empty strings
            word_list = [checksum.strip() for checksum in checksum_list if checksum and checksum.strip()]
            if word_list:
                # Calculate frequencies for each year
                word_frequencies = frequency_of_each_word(word_list)
                # Sum the counts for each checksum type within a year
                checksum_counts = {}
                for checksum, count in word_frequencies:
                    checksum_counts[checksum] = checksum_counts.get(checksum, 0) + count
                # Store the checksum counts as a dictionary for each year
                year_checksums.setdefault(year, {}).update(checksum_counts)

    # Print the year and checksum count for each unique checksum
    for year, checksums in year_checksums.items():
        print(f"Year: {year}")
        for checksum, count in checksums.items():
            print(f"Checksum: {checksum}, Count: {count}")
        print()

    print('---x----')
    print()


def checksum_analysis(cur):
    print_frequency_of_checksums(cur)

    print_frequency_of_checksums_over_years(cur)


def print_frequency_of_checksums(cur):
    # Execute a query to fetch all values from the 'allqualifiers' column in the 'packages' table
    cur.execute("SELECT allchecksum FROM packages")

    # Fetch all the values and store them into the 'all_checksums_list' list
    all_checksums_list = [row['allchecksum'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word(all_checksums_list)

    unique_words_count = len(sorted_frequencies)

    print('CHECKSUM')
    print()

    # Print the word frequencies
    for word, frequency in sorted_frequencies:
        print(f'Checksum: {word} - Frequency: {frequency}')

    print('---x----')
    print()


def frequency_of_each_word(word_list):
    # Create an empty dictionary to store word frequencies
    word_frequencies = {}

    # Iterate through each 'list' value
    for qualifiers_string in word_list:
        if qualifiers_string is not None:
            # Remove the square brackets and split the string into words
            qualifiers_list = qualifiers_string.strip('[]').split(',')

            # Iterate through each word and update the frequencies
            for word in qualifiers_list:
                word = word.strip()  # Remove leading/trailing whitespaces
                if word:
                    word_frequencies[word] = word_frequencies.get(word, 0) + 1

    # Sort the word frequencies by their values in descending order
    sorted_frequencies = collections.Counter(word_frequencies).most_common()

    return sorted_frequencies


main()
