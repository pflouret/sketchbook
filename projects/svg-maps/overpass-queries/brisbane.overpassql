[timeout:30][out:json][maxsize:1073741824];
area(3611677792)(-27.693256, 152.839052, -27.244863, 153.273048);
(
  way[highway~"residential|motorway|primary|secondary|tertiary"](area);
  way[highway~"unclassified|service"](area)(if: length() > 100);
  >;
);
  
out body qt;




