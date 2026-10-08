const collections = ['genres','cinema_rooms','movies','showtimes'];
printjson({ counts: Object.fromEntries(collections.map(c=>[c,db.getCollection(c).countDocuments()])), genreIndexes: db.genres.getIndexes(), roomIndexes: db.cinema_rooms.getIndexes(), priceType: db.showtimes.aggregate([{$limit:1},{$project:{ticketPrice:{$type:'$ticketPrice'}}}]).toArray() });
