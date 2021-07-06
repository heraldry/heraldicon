(ns heraldry.coat-of-arms.semy.options
  (:require [heraldry.coat-of-arms.position :as position]
            [heraldry.frontend.ui.interface :as interface]))

(def default-options
  {:origin (-> position/default-options
               (dissoc :alignment))
   :layout {:num-fields-x {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label "Num columns"}}
            :num-fields-y {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label "Num rows"}}
            :offset-x {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label "Offset x"
                            :step 0.01}}
            :offset-y {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label "Offset y"
                            :step 0.01}}
            :stretch-x {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label "Stretch x"
                             :step 0.01}}
            :stretch-y {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label "Stretch y"
                             :step 0.01}}
            :rotation {:type :range
                       :min -90
                       :max 90
                       :default 0
                       :ui {:label "Rotation"}}
            :ui {:label "Layout"
                 :form-type :semy-layout}}})

(defn options [data]
  (-> default-options
      (update :origin (fn [origin]
                        (when origin
                          (position/adjust-options origin (-> data :origin)))))))

(defmethod interface/component-options :semy [data _path]
  (options data))
