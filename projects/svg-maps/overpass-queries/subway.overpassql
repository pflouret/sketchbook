[timeout:900][out:json][maxsize:1073741824];
rel(175905); // nyc
rel(1224652); // caba
rel(1543125); // tokyo
rel(2555133); // moscow
rel(7444); // paris
rel(913067); // shanghai
rel(2297418); // seoul
rel(65606); // london
rel(10264792); // hong kong
rel(5326784); // madrid
rel(5396194); // dc
rel(111968); // bay area
rel(122604); // chicago
rel(62422); // berlin
rel(1293250); // taipei
rel(406091); // oslo
rel(398021); // stockholm
rel(1320608); // copenhagen
rel(536780); // singapore
rel(44915); // milan
rel(1634158); // montreal
rel(298285); // sao paulo
map_to_area -> .a;

(
  rel[route=subway](area.a);
);
(._;>;);
out body qt;
