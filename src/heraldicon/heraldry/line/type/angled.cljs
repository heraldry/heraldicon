(ns heraldicon.heraldry.line.type.angled)

(def pattern
  {:display-name :string.line.type/angled
   :full? true
   :function (fn [{:keys [width eccentricity] :as _line-data}
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
                  :max height}))})
