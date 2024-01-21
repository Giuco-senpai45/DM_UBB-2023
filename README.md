# Data mining final project
This simulates a part of the Watson IBM system by implementing a search engine that indexes a set of <a href="https://www.dropbox.com/s/nzlb96ejt3lhd7g/wiki-subset-20140602.tar.gz?dl=0"> wikipedia articles </a> and answers questions put in the form of a Jeopardy quiz game.

## Implementation
The project is implemented in Java using Apache's <a href="https://lucene.apache.org"> Lucene </a> for the information retrieval process. And it's built as a Gradle project.

## Building 
You should update the dependencies of the gradle project, to make sure you have the Lucene and CoreNLP packages installed (make sure to <b>run the dependencies gradle task</b>).

Then you can <b>run the build gradle task</b> or you can run the main class from the QueryEngine class.

## Install or Build the index

You can download the index from <a href="https://ubbcluj-my.sharepoint.com/:f:/g/personal/daniel_ardelean_stud_ubbcluj_ro/Eq07AQx1AsdLuAg_TEaLC1ABeW61D4ASa1YvDvkS1lPfyA?e=BRtS65" >here</a> 

Or, after downloading the wikipedia pages to the resources/wiki-data folder, you can run the main function in the QueryEngine class, which will create the index.