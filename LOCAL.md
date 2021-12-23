# How to test install/run locally

## Dependencies
- Java 8 or higher
- `brew install maven`

## Compile
`JAVA_HOME=/usr/local/Cellar/openjdk@11/11.0.12 mvn install -Dmaven.test.skip=true`

## Initial create of empty index
`java -jar target/photon-*.jar -languages nl,en,de,fr,lb -host localhost -port 5432 -database nominatim -user nominatim -nominatim-import`

This created a `photon_data` directory at the current location. To have a clean index, remove this

## Run
`java -jar target/photon-*.jar -languages nl,en,de,fr,lb -host localhost -port 5432 -database nominatim -user nominatim -password <palms nominatim pwd>`

## Insert OSM records
- Optional: `brew services stop postgresql`
- Optional: Save palm's nominatim password locally so you dont have to enter every time `echo "localhost:5432:nominatim:nominatim:<password>" > ~/.pgpass`
- `ssh palm -L 5432:localhost:5432 -N`
- Insert FM-HQ: `./update_id_locally.sh 2775185923 N`

## View created index
- Too really persist data for this viewer (Because of a transaction log) you should shutdown, startup and shutdown again of Photon
- By using Luke: `https://github.com/DmitryKey/luke/releases/download/luke-swing-7.7.0/luke-swing-7.7.0-luke-release.zip`
- Unzip and run: `./luke.sh and choose elasticsearch directory`
