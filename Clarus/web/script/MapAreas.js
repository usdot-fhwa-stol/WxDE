var oMapAreas = eval('([' +
'{"name":"Alabama", "country":"USA", "lat":32.9, "lon":-86.6, "zoom":7, "active":0},' +
'{"name":"Alaska", "country":"USA", "lat":61.2, "lon":-147.3, "zoom":5, "active":1},' +
'{"name":"Arizona", "country":"USA", "lat":34.3, "lon":-112, "zoom":7, "active":1},' +
'{"name":"Arkansas", "country":"USA", "lat":34.8, "lon":-92.3, "zoom":7, "active":0},' +
'{"name":"California", "country":"USA", "lat":37.7, "lon":-119.4, "zoom":6, "active":1},' +
'{"name":"Colorado", "country":"USA", "lat":39, "lon":-105.5, "zoom":7, "active":1},' +
'{"name":"Connecticut", "country":"USA", "lat":41.5, "lon":-72.7, "zoom":9, "active":0},' +
'{"name":"Delaware", "country":"USA", "lat":38.9, "lon":-75.5, "zoom":8, "active":1},' +
'{"name":"Florida", "country":"USA", "lat":28.1, "lon":-84, "zoom":7, "active":1},' +
'{"name":"Georgia", "country":"USA", "lat":32.8, "lon":-83.5, "zoom":7, "active":0},' +
'{"name":"Hawaii", "country":"USA", "lat":20.9, "lon":-157.2, "zoom":7, "active":0},' +
'{"name":"Idaho", "country":"USA", "lat":45.4, "lon":-114.7, "zoom":6, "active":1},' +
'{"name":"Illinois", "country":"USA", "lat":39.9, "lon":-89.2, "zoom":7, "active":1},' +
'{"name":"Indiana", "country":"USA", "lat":40, "lon":-86, "zoom":7, "active":1},' +
'{"name":"Iowa", "country":"USA", "lat":42, "lon":-93.6, "zoom":7, "active":1},' +
'{"name":"Kansas", "country":"USA", "lat":38.3, "lon":-98.4, "zoom":7, "active":1},' +
'{"name":"Kentucky", "country":"USA", "lat":37.9, "lon":-84.4, "zoom":7, "active":1},' +
'{"name":"Louisiana", "country":"USA", "lat":31.4, "lon":-92.5, "zoom":7, "active":0},' +
'{"name":"Maine", "country":"USA", "lat":45.1, "lon":-68.9, "zoom":7, "active":1},' +
'{"name":"Maryland", "country":"USA", "lat":38.9, "lon":-77.3, "zoom":8, "active":1},' +
'{"name":"Massachusetts", "country":"USA", "lat":42.4, "lon":-72.2, "zoom":9, "active":1},' +
'{"name":"Michigan", "country":"USA", "lat":44.1, "lon":-84.8, "zoom":6, "active":1},' +
'{"name":"Minnesota", "country":"USA", "lat":46, "lon":-92.8, "zoom":7, "active":1},' +
'{"name":"Mississippi", "country":"USA", "lat":32.9, "lon":-89.7, "zoom":7, "active":0},' +
'{"name":"Missouri", "country":"USA", "lat":38.3, "lon":-92.4, "zoom":7, "active":1},' +
'{"name":"Montana", "country":"USA", "lat":46.9, "lon":-108.8, "zoom":7, "active":1},' +
'{"name":"Nebraska", "country":"USA", "lat":41.5, "lon":-99.7, "zoom":7, "active":1},' +
'{"name":"Nevada", "country":"USA", "lat":39.4, "lon":-116.6, "zoom":7, "active":1},' +
'{"name":"New Hampshire", "country":"USA", "lat":43.9, "lon":-71.5, "zoom":8, "active":1},' +
'{"name":"New Jersey", "country":"USA", "lat":40.1, "lon":-74.6, "zoom":8, "active":1},' +
'{"name":"New Mexico", "country":"USA", "lat":34.2, "lon":-105.9, "zoom":7, "active":0},' +
'{"name":"New York", "country":"USA", "lat":42.9, "lon":-75.8, "zoom":7, "active":1},' +
'{"name":"North Carolina", "country":"USA", "lat":35.3, "lon":-80, "zoom":7, "active":0},' +
'{"name":"North Dakota", "country":"USA", "lat":47.4, "lon":-100.3, "zoom":7, "active":1},' +
'{"name":"Ohio", "country":"USA", "lat":40, "lon":-82.8, "zoom":7, "active":1},' +
'{"name":"Oklahoma", "country":"USA", "lat":35.5, "lon":-98.6, "zoom":7, "active":1},' +
'{"name":"Oregon", "country":"USA", "lat":44.2, "lon":-120.5, "zoom":7, "active":1},' +
'{"name":"Pennsylvania", "country":"USA", "lat":41, "lon":-77.6, "zoom":7, "active":0},' +
'{"name":"Rhode Island", "country":"USA", "lat":41.7, "lon":-71.5, "zoom":9, "active":0},' +
'{"name":"South Carolina", "country":"USA", "lat":33.8, "lon":-80.6, "zoom":7, "active":1},' +
'{"name":"South Dakota", "country":"USA", "lat":44.5, "lon":-100.3, "zoom":7, "active":1},' +
'{"name":"Tennessee", "country":"USA", "lat":35.9, "lon":-85.9, "zoom":7, "active":1},' +
'{"name":"Texas", "country":"USA", "lat":31.4, "lon":-98.1, "zoom":6, "active":0},' +
'{"name":"Utah", "country":"USA", "lat":39.5, "lon":-111.6, "zoom":7, "active":1},' +
'{"name":"Vermont", "country":"USA", "lat":43.9, "lon":-72.6, "zoom":8, "active":1},' +
'{"name":"Virginia", "country":"USA", "lat":37.8, "lon":-79.5, "zoom":7, "active":1},' +
'{"name":"Washington", "country":"USA", "lat":47.3, "lon":-120.8, "zoom":7, "active":1},' +
'{"name":"West Virginia", "country":"USA", "lat":38.4, "lon":-80.7, "zoom":7, "active":1},' +
'{"name":"Wisconsin", "country":"USA", "lat":44.9, "lon":-89.7, "zoom":7, "active":1},' +
'{"name":"Wyoming", "country":"USA", "lat":43, "lon":-107.6, "zoom":7, "active":1},' +
'{"name":"Alberta", "country":"Canada", "lat":55, "lon":-114.5, "zoom":5, "active":1},' +
'{"name":"British Columbia", "country":"Canada", "lat":55.4, "lon":-121.4, "zoom":5, "active":1},' +
'{"name":"Manitoba", "country":"Canada", "lat":55.1, "lon":-97.5, "zoom":5, "active":0},' +
'{"name":"New Brunswick", "country":"Canada", "lat":46.4, "lon":-65.2, "zoom":7, "active":0},' +
'{"name":"Newfoundland &amp; Labrador", "country":"Canada", "lat":53.9, "lon":-58.6, "zoom":5, "active":0},' +
'{"name":"Northwest Territories", "country":"Canada", "lat":65.8, "lon":-118.5, "zoom":5, "active":0},' +
'{"name":"Nova Scotia", "country":"Canada", "lat":45.1, "lon":-62.5, "zoom":7, "active":1},' +
'{"name":"Nunavut", "country":"Canada", "lat":65.9, "lon":-103.3, "zoom":5, "active":0},' +
'{"name":"Ontario", "country":"Canada", "lat":49.5, "lon":-85.8, "zoom":5, "active":0},' +
'{"name":"Prince Edward Island", "country":"Canada", "lat":46.3, "lon":-63.2, "zoom":8, "active":0},' +
'{"name":"Quebec", "country":"Canada", "lat":53.9, "lon":-68.9, "zoom":5, "active":0},' +
'{"name":"Saskatchewan", "country":"Canada", "lat":55.1, "lon":-104.1, "zoom":5, "active":0},' +
'{"name":"Yukon Territory", "country":"Canada", "lat":64.4, "lon":-132.6, "zoom":5, "active":1}' +
'])');