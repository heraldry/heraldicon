(ns heraldry.coat-of-arms.line.type.angled)

(defn full
  {:display-name "Angled"
   :value :angled
   :full? true}
  [{:keys [width eccentricity] :as _line-data}
   length
   {:keys [real-start real-end] :as _line-options}]
  (let [real-start (or real-start 0)
        real-end (or real-end length)
        relevant-length (- real-end real-start)
        pos-x (-> relevant-length
                  (* eccentricity))
        height width]
    {:pattern ["h" real-start
               "h" pos-x
               "v" height
               "h" (- relevant-length pos-x)
               "h" (- length real-end)
               "v" (- height)]
     :min 0
     :max height}))
