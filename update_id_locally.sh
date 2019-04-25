
if [ "$#" != "2" ]; then
    echo "Usage: $0 <osm_id> <osm_type>"
    echo " osm_type can be: N, R or W"
    exit 1;
fi

ID=$1
TYPE=$2

place_id=$(psql -h localhost -p 5432 -U nominatim -c "SELECT place_id FROM placex WHERE osm_id = $ID AND osm_type = '$TYPE'" -A -t);
wget -q -O- "http://localhost:2322/fm-nominatim-update" --post-data="{ \"delete\": [], \"create\": [$place_id], \"modify\": []}"