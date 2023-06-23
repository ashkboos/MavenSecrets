import collections
import statistics

import psycopg2

from pyscripts.packaging.database import Database


def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    conn = db.connect()
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)

    # Packaging type analysis
    packaging_analysis(cur)

    # Checksum analysis
    checksum_analysis(cur)

    # Qualifier analysis
    qualifier_analysis(cur)

    # Executable analysis
    executable_analysis(cur)

    # Close the cursor and connection
    cur.close()
    conn.close()


def packaging_analysis(cur):
    # Individual frequency of each packaging type from pom
    print_packaging_from_pom(cur)

    # Individual frequency of each packaging type from repo
    print_frequency_from_repo(cur)

    # Individual frequency of each packaging type from index
    print_frequency_from_index(cur)

    # Frequency of packages with frequency of packaging types
    print_frequency_of_packages_with_frequency_packaging_types(cur)

    # Frequency of pairs of difference in packaging type from pom and index
    print_difference_with_individual_freq(cur)

    # Number of different packages where index != repo
    print_frequency_index_packaging_different_repo(cur)


def print_packaging_from_pom(cur):
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

    with open('../Packaging_Type_from_POM.txt', 'w') as file:

        file.write('PACKAGING TYPE FROM POM\n')
        file.write('\n')

        total_count = 0
        for packaging_type, count in packaging_type_counts.items():
            total_count = total_count + count
            file.write(f'Packaging type: {packaging_type}, Count: {count}\n')
        file.write('\n')

        file.write(f'Total number of unique packaging types in pom: {unique_packaging_type_count}\n')
        file.write(f'Total number of packaging types in pom: {total_count}\n')


def print_frequency_from_repo(cur):
    cur.execute("SELECT allpackagingtypefromrepo FROM packages")

    all_packagingtype_list = [row['allpackagingtypefromrepo'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word(all_packagingtype_list)

    unique_words_count = len(sorted_frequencies)

    with open('Packaging_Type_from_REPO.txt', 'w') as file:
        file.write('PACKAGING TYPE FROM THE REPO\n')
        file.write('\n')

        total_count = 0
        # Print the word frequencies
        for word, frequency in sorted_frequencies:
            total_count = total_count + frequency
            file.write(f'Packaging type: {word} - Frequency: {frequency}\n')
        file.write('\n')

        file.write(f'Number of unique packaging types from the repo: {unique_words_count}\n')
        file.write(f'Total number: {total_count}\n')


def print_frequency_from_index(cur):
    cur.execute('SELECT packagingtypefromindex, COUNT(*) FROM packages WHERE packages.packagingtypefromindex IS NOT '
                'NULL GROUP BY '
                'packagingtypefromindex ORDER BY COUNT(*) DESC')
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

    with open('Packaging_Type_from_Index.txt', 'w') as file:

        file.write('PACKAGING TYPE FROM INDEX\n')
        file.write('\n')

        total_count = 0
        for packaging_type, count in packaging_type_counts.items():
            total_count = total_count + count
            file.write(f'Packaging type: {packaging_type}, Count: {count}\n')
        file.write('\n')

        file.write(f'Total number of unique packaging types in index: {unique_packaging_type_count}\n')
        file.write(f'Total number of packaging types in index: {total_count}\n')


def print_frequency_of_packages_with_frequency_packaging_types(cur):
    # Execute a query to fetch all values from the 'allpackagingtype' column in the table
    cur.execute("SELECT allpackagingtypefromrepo FROM packages")

    # Create a dictionary to store the frequency of packaging types for each row
    row_frequency = collections.defaultdict(int)

    # Iterate over the rows and count the distinct packaging types for each row
    for row in cur.fetchall():
        allpackagingtype = row['allpackagingtypefromrepo']
        if allpackagingtype is not None:
            packaging_types = set(allpackagingtype.strip('[]').split(','))
            filtered_packaging_types = set()
            for packaging_type in packaging_types:
                packaging_type = packaging_type.strip()
                if len(packaging_type) > 2 \
                        and not packaging_type.endswith('.xml') \
                        and not packaging_type.endswith('.jar') \
                        and not packaging_type.endswith('.pom') \
                        and not packaging_type.endswith('.asc') \
                        and not packaging_type.startswith('.'):
                    filtered_packaging_types.add(packaging_type)

            num_packaging_types = len(filtered_packaging_types)
            row_frequency[num_packaging_types] += 1

    with open('Frequency_of_packaging_types.txt', 'w') as file:

        file.write('FREQUENCY OF PACKAGES WITH DIFFERENT PACKAGING TYPES\n')
        file.write('\n')

        # Print the frequency of rows with each distinct count of packaging types
        for num_packaging_types, frequency in row_frequency.items():
            file.write(f'Frequency of packages with {num_packaging_types} packaging types: {frequency}\n')


def print_difference_with_individual_freq(cur):
    cur.execute("""
    SELECT packagingtypefrompom, packagingtypefromindex, COUNT(*) FROM packages WHERE 
    packagingtypefrompom != packages.packagingtypefromindex GROUP BY 
    packagingtypefrompom, packagingtypefromindex ORDER BY COUNT(*) DESC
    """)

    results = cur.fetchall()

    with open('Difference_in_packaging_type_POM_Index.txt', 'w') as file:
        file.write('DIFFERENCE IN PACKAGING TYPE IN POM AND INDEX\n')
        file.write('\n')

        total_frequency = 0
        unique_pairs = set()
        for row in results:
            packagingtypefrompom = row[0]
            packagingtypefromrepo = row[1]
            frequency = row[2]
            total_frequency += frequency
            unique_pair = (packagingtypefrompom, packagingtypefromrepo)
            unique_pairs.add(unique_pair)
            file.write(
                f'Packaging type from POM: {packagingtypefrompom}, Packaging type from INDEX: {packagingtypefromrepo}, '
                f'Frequency: {frequency}\n')
        file.write('\n')

        file.write(f'Total Frequency of differences: {total_frequency}\n')
        unique_pair_count = len(unique_pairs)
        file.write(f"Number of unique pairs: {unique_pair_count}\n")


def print_frequency_index_packaging_different_repo(cur):
    # Execute a query to fetch the values from the 'allpackagingtype' and 'packagingtypefromrepo' columns
    cur.execute("SELECT allpackagingtypefromrepo, packagingtypefromindex FROM packages")

    count = 0

    # Iterate over the rows and check if the values are the same
    for row in cur.fetchall():
        allpackagingtype = row['allpackagingtypefromrepo']
        packagingtypefromindex = row['packagingtypefromindex']

        if allpackagingtype is not None:
            # Remove the square brackets and split the 'allpackagingtype' string into a list
            allpackagingtype_list = allpackagingtype.strip('[]').split(',')

            # Remove any leading or trailing whitespaces from each value in the 'allpackagingtype_list'
            allpackagingtype_list = [val.strip() for val in allpackagingtype_list]

            # Check if 'packagingtypefromindex' does not match all the values in 'allpackagingtype_list'
            if set(allpackagingtype_list) != {packagingtypefromindex}:
                count += 1

    with open('Difference_packaging_type_index_repo.txt', 'w') as file:

        file.write(
            f'Number of packages where the packaging type in the index '
            f'is not same as packaging type(s) on repo: {count}')


def checksum_analysis(cur):
    # Frequency of each checksum
    print_frequency_of_checksums(cur)

    # Number of checksum types for each package
    print_frequency_number_of_checksum(cur)

    # Frequency of each checksum in every year
    print_frequency_of_checksums_over_years(cur)


def print_frequency_of_checksums(cur):
    # Execute a query to fetch all values from the 'allchecksum' column in the 'packages' table
    cur.execute("SELECT allchecksumfromrepo FROM packages")

    # Fetch all the values and store them into the 'all_checksums_list' list
    all_checksums_list = [row['allchecksumfromrepo'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word_checksum(all_checksums_list)

    with open('Frequency_of_each_checksum.txt', 'w') as file:
        file.write('CHECKSUM\n')
        file.write('\n')

        # Print the word frequencies
        total_count = 0
        for word, frequency in sorted_frequencies:
            total_count = total_count + frequency
            file.write(f'Checksum: {word} - Frequency: {frequency}\n')

        file.write(f'Total count:{total_count}\n')


def print_frequency_number_of_checksum(cursor):
    query = "SELECT allchecksumfromrepo FROM packages WHERE packages.allpackagingtypefromrepo != '[]'"
    cursor.execute(query)

    # Fetch all rows from the result
    rows = cursor.fetchall()

    # Create a dictionary to store the count of packages for each number of checksum types
    package_count = {}

    # Words to exclude from checksum types
    exclude_words = [".xml", ".jar", ".pom", ".asc"]

    # Iterate through each row and count the distinct number of types
    for row in rows:
        input_string = row[0]  # Retrieve the input string from the database

        if input_string is None:
            continue

        # Evaluate the input string as a literal list
        checksum_list = input_string[1:-1].split(", ")

        # Exclude the specified words from the checksum types
        filtered_checksum_list = [checksum_type for checksum_type in checksum_list if
                                  checksum_type not in exclude_words]

        # Calculate the count of distinct checksum types
        num_types = len(set(filtered_checksum_list))
        package_count[num_types] = package_count.get(num_types, 0) + 1

    with open('Number_of_packages_with_each_checksum.txt', 'w') as file:
        # Display the count of packages for each number of checksum types
        for num_types, count in package_count.items():
            file.write(f'Number of checksum types: {num_types} | Number of packages: {count}\n')


def print_frequency_of_checksums_over_years(cur):
    cur.execute("""
    SELECT EXTRACT(YEAR FROM pl.lastmodified) AS year, p.allchecksumfromrepo
    FROM package_list pl
    JOIN packages p ON pl.groupid = p.groupid
    AND pl.artifactid = p.artifactid
    AND pl.version = p.version;
    """)

    # Fetch all the rows from the result
    result = cur.fetchall()

    # Extract year and checksums
    frequency = collections.defaultdict(lambda: collections.defaultdict(int))
    # Iterate over the query result
    for row in result:
        year = int(row['year'])
        checksums = row['allchecksumfromrepo']
        if checksums is not None:
            checksums = checksums.strip('[]').split(',')
            for checksum in checksums:
                checksum_type = checksum.strip().split('.')[-1]
                if checksum_type in ['md5', 'sha1', 'sha256', 'sha512']:
                    # Increment the frequency count for the checksum in the corresponding year
                    frequency[year][checksum.strip()] += 1

    with open('Frequency_of_checksum_over_years.txt', 'w') as file:

        file.write('FREQUENCY OF EACH CHECKSUM IN EACH YEAR\n')
        file.write('\n')

        # Print the frequency of each checksum in each year
        for year, checksums in frequency.items():
            file.write(f'Year: {year}\n')
            sorted_checksums = sorted(checksums.items(), key=lambda x: x[1], reverse=True)
            for checksum, count in sorted_checksums:
                file.write(f'Checksum: {checksum}, Frequency: {count}\n')


def qualifier_analysis(cur):
    # Execute a query to fetch all values from the 'allqualifiers' column in the 'packages' table
    cur.execute("SELECT allqualifiersfromrepo FROM packages")

    # Fetch all the values and store them into the 'all_qualifiers_list' list
    all_qualifiers_list = [row['allqualifiersfromrepo'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word(all_qualifiers_list)

    unique_words_count = len(sorted_frequencies)

    with open('Frequency_of_each_qualifier.txt', 'w') as file:
        file.write('QUALIFIER\n')
        file.write('\n')

        # Print the word frequencies
        total_count = 0
        frequencies = []
        for word, frequency in sorted_frequencies:
            total_count = total_count + frequency
            frequencies.append(frequency)
            file.write(f'Qualifier: {word} - Frequency: {frequency}\n')
        file.write('\n')

        file.write(f'Number of unique qualifiers: {unique_words_count}\n')
        file.write(f'Total count:{total_count}\n')
        median_frequency = statistics.median(frequencies)
        file.write(f'Median of qualifiers: {median_frequency}\n')


def executable_analysis(cur):
    # Execute a query to fetch all values from the 'typeoffile' column in the 'packages' table
    cur.execute("SELECT typesoffile FROM packages")

    # Fetch all the values and store them into the 'all_qualifiers_list' list
    all_file_type_list = [row['typesoffile'] for row in cur.fetchall()]

    sorted_frequencies = frequency_of_each_word(all_file_type_list)

    unique_words_count = len(sorted_frequencies)

    with open('Frequency_of_each_file_type.txt', 'w') as file:
        file.write('TYPE OF FILE\n')
        file.write('\n')

        # Print the word frequencies
        total_count = 0
        for word, frequency in sorted_frequencies:
            total_count = total_count + frequency
            file.write(f'File type: {word} - Frequency: {frequency}\n')
        file.write('\n')

        file.write(f'Number of unique file types: {unique_words_count}\n')
        file.write(f'Total count:{total_count}\n')


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
                # Exclude one-letter words, empty words, and words ending with ".xml"
                if len(word) > 2 and word != "" \
                        and not word.endswith(".xml") \
                        and not word.endswith('.jar') \
                        and not word.endswith('.pom') \
                        and not word.endswith('.asc') \
                        and not word.startswith('.'):
                    word_frequencies[word] = word_frequencies.get(word, 0) + 1

    # Sort the word frequencies by their values in descending order
    sorted_frequencies = collections.Counter(word_frequencies).most_common()

    return sorted_frequencies


def frequency_of_each_word_checksum(word_list):
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
                # Exclude one-letter words, empty words, and words ending with ".xml"
                if len(word) > 2 and word != "" \
                        and not word.endswith(".xml") \
                        and not word.endswith('.jar') \
                        and not word.endswith('.pom') \
                        and not word.endswith('.asc'):
                    word_frequencies[word] = word_frequencies.get(word, 0) + 1

    # Sort the word frequencies by their values in descending order
    sorted_frequencies = collections.Counter(word_frequencies).most_common()

    return sorted_frequencies


main()
