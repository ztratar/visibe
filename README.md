# visibe

```
lein run help

lein test # assumes you have a mongo instance running on localhost with default mongo configuration. EG: port 27017, database local
```

# TODO

- duplicates are being pulled in, detect the. We are currently getting buckets. Simply putting them into a set won't work becuase they could have different time stamps
- A trend from last week is not = to a trend this week (use datomic?)
- Use the browser history API 
- Only fetch images from flickr that are of a certain size
- We're not catching exceptions anywhere in this program, which is NOT OKAY
- Remove implicit time sorting nonsense
- Source maps
- codox
- Apply the insights from SS's workflow http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded
- UI updates
- Save everything to the database
- Sharing functionality
- Deploy
- Vines

## License

Copyright © 2013 VISIBE