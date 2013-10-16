(ns eve.d3)

;; (defn clear! []
;;   (.remove (.selectAll svg "circle"))
;;   (.remove (.selectAll svg "image")))

;; (defn svg! []
;;     (def svg (-> d3
;;                (.select "body")
;;                (.append "svg")
;;                (.attr "width" 700)
;;                (.attr "height" 700))))

;; (defn trend-intro [img-uri]
;;   ;; Add circle
;;   (-> svg
;;       (.append "circle")
;;       (.attr "x" 200)
;;       (.attr "y" 200)
;;       (.attr "cx" 0)
;;       (.attr "cy" 0)
;;       (.attr "r" 0)
;;       (.attr "class" "circle"))

;;   ;; Add Image
;;   (-> svg
;;       (.append "image")
;;       (.attr "xlink:href" "breaking-bad.png")
;;       (.attr "x" 60)
;;       (.attr "y" 60)
;;       ;; (.attr "width" 100)
;;       ;; (.attr "height" 100)
;;       (.attr "opacity" 0))

;;   (-> svg
;;       (.select "circle")
;;       (.transition)
;;       (.duration 3000)
;;       (.attr "x" 250)
;;       (.attr "y" 250)
;;       (.attr "cx" 250)
;;       (.attr "cy" 250)
;;       (.attr "r" 100)
;;       (.ease "elastic"))

;;   (-> svg
;;       (.select "image")
;;       (.transition)
;;       (.duration 3000)
;;       (.attr "opacity" 1)))


;; ;; Add image
;; (-> svg
;;     (.append "image")
;;     (.attr "xlink:href" "http://localhost:9000/breaking-bad.jpg")
;;     (.attr "clip-path" "url(#clipping)")
;;     (.attr "x" 0)
;;     (.attr "y" 0)
;;     (.attr "width" 200)
;;     (.attr "height" 200))

;; ;; Transitions
;; (-> d3
;;     (.selectAll "circle")
;;     (.transition)
;;     (.duration 3000)
;;     (.attr "cx" 100)
;;     (.attr "cy" 100)
;;     (.attr "r" 100)
;;     (.attr "opacity" 1)
;;     (.ease "gradual"))

;;;
;;;
;;; 

;; (def n 40)
;; (def my-random (.normal (.-random d3) 0 .2))
;; (def data (.map (.range d3 n) my-random))

;; (def margin {:top 20 :right 20 :bottom 20 :left 40})
;; (def width (- 960 (:left margin) (:right margin)))
;; (def height (- 500 (:top margin) (:bottom margin)))

;; (def x (-> d3
;;            (.-scale)
;;            (.linear)
;;            (.domain (array 0 (- n 1)))
;;            (.range  (array 0 width))))

;; (def y (-> d3
;;            (.-scale)
;;            (.linear)
;;            (.domain (array (- 1) 1))
;;            (.range  (array height 0))))

;; (def line (-> d3
;;               (.-svg)
;;               (.line)
;;               (.x (fn [_ i] (x i)))
;;               (.y (fn [d _] (x d)))))

;; (def svg (-> d3
;;              (.select "body")           ;Select all against the stuff
;;              (.append "svg")
;;              (.attr )                   ; then append everything
;;              (.attr "width" (+ width (:left margin) (:right margin)))
;;              (.attr "width" (+ height (:top margin) (:bottom margin)))

;;              (.append "g")
;;              (.attr "transform"
;;                     (str "translate(" (:left margin) "," (:top margin) ")"))))

;; (-> svg
;;     (.append "defs")
;;     (.append "clipPath")
;;     (.attr "id" "clip")
;;     (.append "rect")
;;     (.attr "width" width)
;;     (.attr "height" height))

;; (-> svg
;;     (.append "g")
;;     (.attr "class" "x axis")
;;     (.attr "transform" (str "translate(0," (y 0) ")"))
;;     (.call (-> (.-svg d3)
;;                (.axis)
;;                (.scale x)
;;                (.orient "bottom"))))

;; (-> svg
;;     (.append "g")
;;     (.attr "class" "y axis")
;;     (.call (-> (.-svg d3)
;;                (.axis)
;;                (.scale y)
;;                (.orient "left"))))

;; (def path (-> svg
;;               (.append "g")
;;               (.attr "clip-path" "url(#clip)")
;;               (.append "path")
;;               (.datum data)
;;               (.attr "class" "line")
;;               (.attr "d" line)))

;; (defn tick []
;;   (.push data my-random)

;;   (-> path
;;       (.attr "d" line)
;;       (.attr "transform" nil)
;;       (.transition)
;;       (.duration 500)
;;       (.ease "linear")
;;       (.attr "transform" (str "translate(") (x (- 1)) ",0)")
;;       (.each "end" tick))

;;   (.shift data))

;; (tick)

;;;
;;;
;;; 

;; (-> svg
;;     (.append "defs")
;;     (.append "clipPath")
;;     (.attr "id" "clip")
;;     (.append "rect")
;;     (.attr "width" width)
;;     (.attr "height" height))

;; (-> svg
;;     (.append "g")
;;     (.attr "class" "x axis")
    ;; (.attr "transform" (str "translate(0") (y 0) ")")
;;     (.call (.orient (.scale (.axis (.-svg d3)) x) "bottom")))

;; (-> svg
;;     (.append "g")
;;     (.attr "class" "y axis")
;;     (.call (.orient (.scale (.axis (.-svg d3)) y) "left")))

;; (def path (-> svg
;;               (.append "g")
;;               (.attr "clip-path" "url(#clip)")
;;               (.append "path")
;;               (.datum data)
;;               (.attr "class" "line")
;;               (.attr "d" line)))


;;; Image masking
;;; http://bl.ocks.org/GerHobbelt/3696645

;; (-> svg
;;     (.append "image")
;;     (.attr "xlink:href" "http://localhost:9000/test_img.png")
;;     (.attr "x" 60)
;;     (.attr "y" 60)
;;     (.attr "width" 100)
;;     (.attr "height" 100)
;;     (.clip "rect(10px, 10px, 10px, 10px)"))

;; (-> svg
;;     (.append "clipPath")
;;     (.attr "id" "clipping")
;;     (.attr "cx" 100)
;;     (.attr "cy" 100)
;;     (.attr "r" 100))

;; (defn add-image-circle
;;   "Adds the image circle to the DOM and"
;;   [url]
;;   ;; Add circle
;;   (-> svg
;;       (.append "circle")
;;       (.attr "id" "circle-clipper")
;;       (.attr "cx" 50)
;;       (.attr "cy" 50)
;;       (.attr "r" 50)
;;       (.attr "opacity" 0)
;;       (.attr "class" "circle"))

;;   ;; Add image
;;   (-> svg
;;       (.append "image")
;;       (.attr "xlink:href" "http://localhost:9000/breaking-bad.jpg")
;;       (.attr "clip-path" "url(#clipping)")
;;       (.attr "x" 0)
;;       (.attr "y" 0)
;;       (.attr "width" 200)
;;       (.attr "height" 200))

;;   ;; Transitions
;;   (-> d3
;;       (.selectAll "circle")
;;       (.transition)
;;       (.duration 3000)
;;       (.attr "cx" 100)
;;       (.attr "cy" 100)
;;       (.attr "r" 100)
;;       (.attr "opacity" 1)
;;       (.ease "gradual")))

;; (defn add-feed-path []
;;   (-> svg
;;       (.append "rect")
;;       (.attr "fill" "#3e80ab")
;;       (.attr "x" "300px")
;;       (.attr "y" 0)
;;       (.attr "width" 30)
;;       (.attr "height" "100%")))

;; (-> svg
;;     (.append "image")
;;     (.style "preserveAspectRatio" "xMidyMid slice")
;;     (.style "clip-path" "url(#circle-clipper)")
;;     (.attr "xlink:href" "revy-silhouette.jpg"))

;; (-> svg
;;     (.append "clipPath")
;;     (.attr "id" "circle-clipper")
;;     )

;; (dommy/append! (m/sel1 "body")

;;                (m/node [:svg
;;                         [:defs
;;                          [:linearGradient#gradient {:x1 "0" :y1 "0" :x2 "0" :y2 "100%"}
;;                           [:stop {:stop-color "black" :offset "0"}]
;;                           [:stop {:stop-color "white" :offset "1"}]]
                         
;;                          [:mask#masking {:maskUnits "objectBoundingBox"
;;                                          :maskContentUnits "objectBoundingBox"}
;;                           [:rect {:y "0.3" :width "1" :height ".7" :fill "url(#gradient)"}]
;;                           [:circle {:cx ".5" :cy ".5" :r ".35" :fill "white"}]]]]))

;; (dommy/append! (m/sel1 "body") (m/node [:img {:src "http://localhost:9000/breaking-bad.jpg"}]))


;; (.map (.selectAll d3 ".trend-card")
;;       (fn [x] (-> x
;;                   (.append "circle")
;;                   (.attr "id" "circle-clipper")
;;                   (.attr "fill" "red")
;;                   (.attr "cx" 50)
;;                   (.attr "cy" 50)
;;                   (.attr "r" 50)
;;                   (.attr "opacity" 0)
;;                   (.attr "class" "circle"))))

;; (-> svg
;;     (.append "circle")
;;     (.attr "id" "circle-clipper")
;;     (.attr "cx" 50)
;;     (.attr "cy" 50)
;;     (.attr "r" 50)
;;     (.attr "opacity" 0)
;;     (.attr "class" "circle"))

;; (def margin {:top 10 :right 10 :bottom 100 :left 40})
;; (def margin-2 {:top 430 :right 10 :bottom 20 :left 40})
;; (def width (- 960 (:left margin) (:right margin)))
;; (def height (- 500 (:top margin) (:bottom margin)))
;; (def height-2 (- 500 (:top margin-2) (:bottom margin-2)))

;; (def parse-date (.-parse (.format (.-time d3) "%b %Y")))

;; (def x (.range (.scale (.-time d3)) (array 0 width)))
;; (def x-2 (.range (.scale (.-time d3)) (array 0 width)))
;; (def y (.range (.scale (.-time d3)) (array height 0)))
;; (def y-2 (.range (.scale (.-time d3)) (array height-2 0)))

;; (def svg (-> d3
;;              (.select "#content")
;;              (.append "svg")
;;              (.attr "width" (+ width (:left margin) (:right margin)))
;;              (.attr "height" (+ height (:top margin) (:bottom margin)))))

;; (def focus (.attr (.append svg "g")
;;                   "transform" (str "translate(" (:left margin) "," (:top margin) ")")))

;; (def x-axis (-> (.-svg d3)
;;                 (.axis)
;;                 (.scale x)
;;                 (.orient "bottom")))

;; (def x-axis-2 (-> (.-svg d3)
;;                   (.axis)
;;                   (.scale x-2)
;;                   (.orient "bottom")))

;; (def y-axis (-> (.-svg d3)
;;                 (.axis)
;;                 (.scale y)
;;                 (.orient "left")))

;; (def brush (-> (.-svg d3)
;;                (.brush)
;;                (.x x-2)
;;                (.on "brush" brushed)))

;; (defn brushed []
;;   (.domain x (if (.empty brush) (.domain x-2) (.extent brush)))
;;   (.attr (.select focus "path") "d" area)
;;   (.call (.select focus ".x.axis") x-axis))

;; (def context (.attr (.append svg "g")
;;                     "transform"
;;                     (str "translate("
;;                          (:left margin-2)
;;                          ","
;;                          (:top margin-2) ")")))

;; (def area (-> (.area (.-svg d3))
;;               (.interpolate "monotone")
;;               (.x (fn [d] (x (.-date d))))
;;               (.y0 height)
;;               (.y1 (fn [d] (y (.-price d))))))

;; (def area-2 (-> (.area (.-svg d3))
;;                 (.interpolate "monotone")
;;                 (.x (fn [d] (x-2 (.-date d))))
;;                 (.y0 height-2)
;;                 (.y1 (fn [d] (y-2 (.-price d))))))

;; (-> (.append svg "defs")
;;     (.append "clipPath")
;;     (.attr "id" "clip")
;;     (.append "rect")
;;     (.attr "width" width)
;;     (.attr "height" height)) 

;; (defn test-chart []
;;   ((.-csv d3)
;;    "sp500.csv"
;;    (fn [error data]
;;      (doseq [d data]
;;        (set! (.-date d) (parse-date (.-date d)))
;;        (set! (.-price d) (js/parseFloat (.-price d))))
     
;;      (.domain x (.extent d3 (.map data (fn [d] (.-date d)))))
;;      (.domain y (array 0 (.max d3 (.map data (fn [d] (.-price d))))))
;;      (.domain x-2 (.domain x))
;;      (.domain y-2 (.domain y))

;;      (-> focus
;;          (.append "path")
;;          (.datum data)
;;          (.attr "clip-path" "url(#clip)")
;;          (.attr "d" area))

;;      (-> focus
;;          (.append "g")
;;          (.attr "class" "x axis")
;;          (.attr "transform" (str "translate( 0 ," height ")"))
;;          (.call x-axis))

;;      (-> focus
;;          (.append "g")
;;          (.attr "class" "y axis")
;;          (.call y-axis))

;;      (-> context
;;          (.append "path")
;;          (.datum data)
;;          (.attr "d" area-2))

;;      (-> context
;;           (.append "g")
;;          (.attr "class" "x axis")
;;          (.attr "transform" (str "translate( 0 ," height-2 ")"))
;;          (.call x-axis-2))

;;      (-> context
;;          (.append "g")
;;          (.attr "class" "x brush")
;;          (.call brush)
;;          (.selectAll "rect")
;;          (.attr "y" -6)
;;          (.attr "height" (+ height-2 7)))
;;      )))


