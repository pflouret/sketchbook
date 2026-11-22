[timeout:300][out:json][maxsize:1073741824];
(
area(3603333281); // Shire of Mornington Peninsula
area(3603333252); // City of Frankston
area(3603333276); // City of Casey
area(3602405038); // City of Manningham
area(3602432416); // City of Wyndham
area(3603333042); // City of Bayside
area(3603333046); // City of Kingston
area(3602404870); // City of Melbourne
area(3602404889); // City of Port Phillip
area(3602432415); // City of Hobsons Bay
area(3603327554); // City of Brimbank
area(3603327767); // City of Merri-bek
area(3603329056); // City of Moonee Valley
area(3603330965); // City of Maribyrnong
area(3602403625); // City of Banyule
area(3602404379); // City of Darebin
area(3602404473); // City of Boroondara
area(3602404491); // City of Yarra
area(3602404995); // City of Stonnington
area(3602406273); // City of Whitehorse
area(3602905535); // City of Monash
area(3603331087); // City of Glen Eira
area(3603333235); // City of Greater Dandenong
area(3603333971); // City of Knox
) -> .a;

(
  //way[highway~"unclassified|service"](area.a)(if: length() > 100);
  //way[highway=footway][footway!=sidewalk](area.a)(if: length() > 80);
  way[highway~"(motorway|primary|secondary|tertiary)$"](area.a);
  way[highway~"link"](area.a)(if: length() > 50);
  way[highway~"unclassified"](area.a)(if: length() > 100);
  way[highway=residential](area.a)(if: length() > 100);
);
(._;>;);

out body qt;
