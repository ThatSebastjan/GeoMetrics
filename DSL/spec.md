# Draft for our domain specific language

## TODO: proper markdown formatting

```
base numberic type: double

Point type: (longitude, latitude)

Data types:
    -Number: double
    -Point: (longitude, latitude)   //longitude, latitude = Number (expression)
    -CadastreID: String; Regex: \d+
    -LotID: String; Regex: (\d+\/\d+)|(\d+)

    -Fraction: Number in range 0.0 to 1.0

    -UnitType: null
    -PropertyType: String | Number | UnitType
    -VariableType: Number | Point | Polygon

    -Bounds: 
        -Polygon(Point, Point, ..., Point),             //list of points (start point = end point)
        -Circle(Point, Number),                         //center, radius
        -Box(Point, Point),                             //top left, bottom right
        -PolyLine(Number, Point, Point, ..., Point)     //width, points ...

    -LotType:
        Road,
        BuildingLot,
        FarmLand,
        Forest,
        Meadow,
        River,
        Lake,

    -RiskType:
        Flood,
        LandSlide,
        Earthquake

    
```


## Sample program format
```
//Cadastre is the outer most block.
cadastre {

    const cadastreBounds = import "./cadastreBounds.json"; //Import only supports a list of polygons

    .name: "nekaj"; // comment 
    .id: CadastreID;
    .bounds: cadastreBounds[0];
    .someOtherProperty: PropertyType;
    .test-prop: 123;


    //Lot -> base type for every other type. Identified by .type property
    lot {
        .id: LotID;
        .type: LotType;
        .bounds: Bounds;

        .property: PropertyType; //Additional property
    }

    //Syntactic sugar for "lot" block above
    road(LotID, Bounds);
    buildingLot(LotID, Bounds);
    farmLand(LotID, Bounds);
    forest(LotID, Bounds);
    meadow(LotID, Bounds);
    river(LotID, Bounds);
    lake(LotID, Bounds);



    //Constants
    const varName = VariableType;
    const p1 = (1.73, 1.41);
    const otherVar = (p1.longitude + 2, p1.latitude - 1);


    
    //Lambda expressions
    const makeBounds = lambda(x, y, w, h) {
        return Box((x, y), (x + w, y + h));
    };

    meadow("123/4", makeBounds(10, 10, 12, 14)); //Example usage

}


//Risk can be defined globally in the outer most block
risk {
    .type: RiskType;
    .bounds: Bounds;
    .probability: Fraction;
    .frequency: Fraction;
}

//Risk syntax sugar
flood(Bounds, Fraction, Fraction);      //bounds, probability, frequency
landSlide(Bounds, Fraction, Fraction);
earthQuake(Bounds, Fraction, Fraction);



const externData = import "./extern.json";

//Foreach loop
foreach poly in externData {
    
    //If statement
    if(area(poly) > 1000){ //area = inbuilt function

        risk {
            .type: RiskType;
            .bounds: poly;
            .probability: 0.5;
            .frequency: i / 10;
        }
    }
}


```



## Things to consider:
 - Loading bounds from a json file
 - null type doesn't make sense as a property value. Why event have the property then??