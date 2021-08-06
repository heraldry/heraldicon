(ns heraldry.ribbon)

(def default-options
  {:thickness {:type :range
               :default 10
               :min 5
               :max 30
               :ui {:label "Thickness"
                    :step 0.1}}})

(defn options [data]
  default-options)

