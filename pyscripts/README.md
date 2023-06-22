# PyScripts
Various python scripts to dump data.



## [Sizeplots](vc/sizeplots.py)
To obtain the desired graphs for the size and dependency extractor, run this file. 
Make sure to change the Database variables to their corresponding values (name, port, username, password)
and change the SQL statement to match your table name (default is packages).

Running the sizeplots will yield 4 graphs. The first one displays the size distribution
of packages in the ecosystem. Make sure to save it if desired, when you close the image, the next
graph shows up which is a scatterplot between the number of files and size in an artifact.
Thereafter, the same graphs for direct and transitive dependencies will be shown.

You might have to tweak the number of bins or the x-limit for the first graph 'plot_size_distribution(size)' depending
on your sample size.

## SQL queries used for excel graphs
Not all graphs were made with python. The other graphs were made by running the following SQL queries 
on the database after which the results were inserted into excel to make a graph there.

Obtain the distribution of packages selected per year:

~~~postgresql
SELECT extract(year from lastmodified) as year, count(*) as count_per_year
FROM selected_packages
GROUP BY year
ORDER BY year;
~~~

Obtain the average size of an artifact per year the artifact was last modified:

~~~postgresql
SELECT EXTRACT(YEAR FROM t1.lastmodified) AS year, AVG(t2.size /1000) AS average_size
FROM package_list AS t1
         JOIN packages AS t2 ON t1.groupid = t2.groupid AND t1.artifactid = t2.artifactid AND t1.version = t2.version
GROUP BY year
ORDER BY year;
~~~

Get the 20 file extensions with the largest average size:

~~~postgresql
SELECT extension, avg((size / 1000) / extensions.count) AS mean_size
FROM extensions
GROUP BY extension
ORDER BY mean_size DESC
LIMIT 20;
~~~

Total number of occurrences for top 20 file extensions:
~~~postgresql
SELECT extension, sum(count) as occ
FROM extensions
GROUP BY extension
order by occ DESC
LIMIT 20;
~~~