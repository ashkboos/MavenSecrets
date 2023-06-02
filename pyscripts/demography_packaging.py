import collections

import matplotlib.pyplot as plt
import psycopg2

from pyscripts.database import Database


def main():
    db = Database('localhost', '5432', 'postgres', 'SuperSekretPassword')
    conn = db.connect()
    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)

    # Packaging type analysis
    packaging_analysis(cur)

    # Qualifier analysis
    frequency_of_each_qualifier(cur)

    # Close the cursor and connection
    cur.close()
    conn.close()


def packaging_analysis(cur):
    # Frequency of pairs of difference in packaging type from and index
    # TODO - NEED TO UPDATE
    result_difference = get_frequency_of_difference(cur)
    print_result_with_individual_freq(result_difference)

    # Individual frequency of each packaging type from pom
    results_frequency_pom = get_frequency_from_pom(cur)
    print_plot_packaging_pom(results_frequency_pom)


def get_frequency_of_difference(cur):
    cur.execute('SELECT packagingtypefrompom, packagingtypefromrepo, COUNT(*) FROM packages WHERE '
                'packagingtypefrompom != packagingtypefromrepo GROUP BY '
                'packagingtypefrompom, packagingtypefromrepo')

    results = cur.fetchall()
    return results


def print_result_with_individual_freq(results):
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

    print(f'Total Frequency of differences: {total_frequency}')
    print('---x----')


def get_frequency_from_pom(cur):
    cur.execute('SELECT packagingtypefrompom, COUNT(*) FROM packages WHERE packagingtypefrompom IS NOT NULL GROUP BY '
                'packagingtypefrompom')
    results = cur.fetchall()
    return results


def print_plot_packaging_pom(results_frequency):
    # Create a dictionary to store the "packagingType" values and their counts
    packaging_type_counts = {}

    for row in results_frequency:
        packaging_type = row[0]
        count = row[1]
        packaging_type_counts[packaging_type] = count

    total_count = sum(packaging_type_counts.values())

    print('PACKAGING TYPE FROM POM')
    print()

    for packaging_type, count in packaging_type_counts.items():
        print(f'Packaging type: {packaging_type}, Count: {count}')
    print(f'Total number of packages: {total_count}')
    print('---x----')

    # Create a bar chart
    packaging_type_counts = {key: value for key, value in packaging_type_counts.items() if value is not None}
    plt.bar(packaging_type_counts.keys(), packaging_type_counts.values())

    # Add labels and title
    plt.xlabel('Packaging Type')
    plt.ylabel('Count')
    plt.title('Number of Packages by Packaging Type')
    plt.xticks(rotation=45, ha='right')
    x_tick_labels = [packaging_type[:5] + '...' if len(packaging_type) > 5 else packaging_type for packaging_type in
                     packaging_type_counts.keys()]
    plt.xticks(list(packaging_type_counts.keys()), x_tick_labels)

    # Show the chart
    plt.show()


def frequency_of_each_qualifier(cur):
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
        print(f"Word: {word} - Frequency: {frequency}")

    print(f"Number of unique qualifiers: {unique_words_count}")
    print('---x----')


def frequency_of_each_word(all_qualifiers_list):
    # Create an empty dictionary to store word frequencies
    word_frequencies = {}

    # Iterate through each 'allqualifiers' value
    for qualifiers_string in all_qualifiers_list:
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
