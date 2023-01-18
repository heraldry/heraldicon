(ns heraldicon.frontend.highlight)

(def ^:private pattern-id
  "selected-pattern")

(def fill-url
  (str "url(#" pattern-id ")"))

(defn defs [& {:keys [scale]
               :or {scale 1}}]
  [:defs
   (let [size (* 2 scale)
         r (* 0.25 scale)]
     [:pattern {:id pattern-id
                :width size
                :height size
                :pattern-units "userSpaceOnUse"}
      [:g.area-highlighted
       [:circle {:cx 0
                 :cy 0
                 :r r}]
       [:circle {:cx size
                 :cy 0
                 :r r}]
       [:circle {:cx 0
                 :cy size
                 :r r}]
       [:circle {:cx size
                 :cy size
                 :r r}]
       [:circle {:cx (/ size 2)
                 :cy (/ size 2)
                 :r r}]]])])
