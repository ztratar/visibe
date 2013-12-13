# visibe

```
ssh root@192.81.214.133 # adommknqksgf

lein run help
```

# Relevent technologies / links for me to use 

- http://marcopolo.io/2013/10/01/servant-cljs.html
- http://writingcoding.blogspot.com/search/label/clojure-series
- http://writequit.org/blog/?p=351
- http://nlp.stanford.edu/
- http://nlpwp.org/book/
- http://www.amazon.com/Foundations-Statistical-Natural-Language-Processing/dp/0262133601
- http://www.goodreads.com/shelf/show/natural-language-processing
- http://dinosrc.it/
- http://www.quora.com/Natural-Language-Processing/What-are-the-most-important-research-papers-which-all-NLP-students-should-definitely-read

# Prior to release:

- we need to have a database that won't have it's limit exceeded within 24 hrs
- strong password protection

# TODO

## Major

- Graphs
- Sharing functionality
- synopis http://www.yelp.com/biz/metro-balderas-1-san-jose#query:tacos

## Minor

- Twitter Videos
- multiple links within a tweet - get URLs out of the corresponding key in tweets

- Vines

- loop and update datum times
- preloaders
- get the number of pixes per colum to determine which column to place the next datum instead of the current hack
- datums per second toggle

- codox
- slamhound
- Apply the insights from SS's workflow http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded

## Bugs

- Some tweets are not showing up - I would bet that they are the twitter photos
- Using HTML5 history will not unsubscribe/subscribe you from trends as expected.
- We will, on occasion, see a preloader AND some datums
- Historical datums don't work perfectly
- New datums are being appended, even if they are the same as the old datum. This rarely happens.
- A trend from last week is not = to a trend this week
- not catching exceptions anywhere in this program

## Misc

- Eric want's to have the datums take up less space. 

- most commmon keywords + an example tweet

## License

Copyright © 2013 VISIBE
