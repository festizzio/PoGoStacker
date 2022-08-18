# PoGoStacker
This program can record and modify your saved research rewards for Pokemon GO.

## The future is now!
Have you ever hesitated to claim a new research task, for fear of losing that Charizard? 
Or how about not realizing you just lost 3000 stardust because you didn't know you had already hit your 99 Pokemon limit until it was too late?
Not anymore! With the PoGoStacker app, you can record these in an easy-to-use application that also tells you the potential IVs of each Pokemon as
well as your total stardust value (with no modifiers). 

## Running the application
Clone the repository as a Maven project and allow Maven to resolve all dependencies. JavaFX, JDBC for SQLite, JUnit for testing, and JSoup for scraping data from TheSilphRoad.com and pokemongo.fandom.com. 
When first running the application, the stack will be empty as you haven't claimed anything yet. Once you claim your first reward, the table to the right of the main application window will populate with the new Pokemon, its CP, and its potential IV percentages. 
Use the MainStarter class to run as using Main directly will cause the app to crash while loading.
