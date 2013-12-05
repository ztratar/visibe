(ns visibe.npl
  "NLP stuff for visibe"
  (:use opennlp.nlp
        opennlp.treebank))

(def penn-treebank-parts-of-speech
  ;; "Number" "Tag" "Description"
  [[1	"CC"	"Coordinating conjunction"]
   [2	"CD"	"Cardinal number"]
   [3	"DT"	"Determiner"]
   [4	"EX"	"Existential there"]
   [5	"FW"	"Foreign word"]
   [6	"IN"	"Preposition or subordinating conjunction"]
   [7	"JJ"	"Adjective"]
   [8	"JJR"	"Adjective, comparative"]
   [9	"JJS"	"Adjective, superlative"]
   [10	"LS"	"List item marker"]
   [11	"MD"	"Modal"]
   [12	"NN"	"Noun, singular or mass"]
   [13	"NNS"	"Noun, plural"]
   [14	"NNP"	"Proper noun, singular"]
   [15	"NNPS"	"Proper noun, plural"]
   [16	"PDT"	"Predeterminer"]
   [17	"POS"	"Possessive ending"]
   [18	"PRP"	"Personal pronoun"]
   [19	"PRP$"	"Possessive pronoun"]
   [20	"RB"	"Adverb"]
   [21	"RBR"	"Adverb, comparative"]
   [22	"RBS"	"Adverb, superlative"]
   [23	"RP"	"Particle"]
   [24	"SYM"	"Symbol"]
   [25	"TO"	"to"]
   [26	"UH"	"Interjection"]
   [27	"VB"	"Verb, base form"]
   [28	"VBD"	"Verb, past tense"]
   [29	"VBG"	"Verb, gerund or present participle"]
   [30	"VBN"	"Verb, past participle"]
   [31	"VBP"	"Verb, non-3rd person singular present"]
   [32	"VBZ"	"Verb, 3rd person singular present"]
   [33	"WDT"	"Wh-determiner"]
   [34	"WP"	"Wh-pronoun"]
   [35	"WP$"	"Possessive wh-pronoun"]
   [36	"WRB"	"Wh-adverb"]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Articles and google scraping

;;; grab the first result from google for now. Then add it to the app, ask Zach
;;; what he thinks - should we cross reference it with the links coming back
;;; from twitter? Or just get the most common of the links coming back from
;;; twitter and have that as the article?

;;; Feedzilla is another option - correlate them?

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; NLP

(def tokenize (make-sentence-detector "models/en-sent.bin"))
(def tokenize (make-tokenizer "models/en-token.bin"))
(def detokenize (make-detokenizer "models/english-detokenizer.xml"))
(def pos-tag (make-pos-tagger "models/en-pos-maxent.bin"))
(def name-find (make-name-finder "models/namefind/en-ner-person.bin"))
(def chunker (make-treebank-chunker "models/en-chunker.bin"))

(defn chunk-tweet [tweet] (noun-phrases (chunker (pos-tag (tokenize tweet)))))
(defn tweet->noun-phrases [tweet] (noun-phrases (chunker (pos-tag (tokenize tweet)))))

;; (tweet->noun-phrases)

;; (filter (fn [[_ n]] (> n 10)) (into [] (frequencies (reduce into toy-data))))

;; (def test-stuff (map (keys (visibe.state/gis [:google :trends]))))

;; (def test-data (map #(:statuses (visibe.feeds.twitter/search-tweets %))
;;                    (keys (visibe.state/gis [:google :trends]))))

;; (ppn (map #(noun-phrases (chunk-tweet %))
;;           (take 2 (first tweet-text))))

;; (map #(:statuses (visibe.feeds.twitter/search-tweets %))
;;      (keys (visibe.state/gis [:google :trends])))

;; (ppn (map #(noun-phrases (chunk-tweet %))
;;           (filter string? (first tweet-text))))

;; (def play-tweets (filter string? (map :text (filter (#{"tweet" "tweet-photo"} (:datum-type %))
;;                                                     (visibe.feeds.storage/datums-for "Frozen")))))
