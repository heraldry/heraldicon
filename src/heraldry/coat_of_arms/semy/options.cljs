(ns heraldry.coat-of-arms.semy.options
  (:require
   [heraldry.coat-of-arms.position :as position]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def default-options
  {:origin (-> position/default-options
               (dissoc :alignment))
   :layout {:num-fields-x {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label strings/num-columns}}
            :num-fields-y {:type :range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui {:label strings/num-rows}}
            :offset-x {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label (util/str-tr strings/offset " x")
                            :step 0.01}}
            :offset-y {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label (util/str-tr strings/offset " x")
                            :step 0.01}}
            :stretch-x {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label (util/str-tr strings/stretch " x")
                             :step 0.01}}
            :stretch-y {:type :range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui {:label (util/str-tr strings/stretch " y")
                             :step 0.01}}
            :rotation {:type :range
                       :min -90
                       :max 90
                       :default 0
                       :ui {:label strings/rotation}}
            :ui {:label strings/layout
                 :form-type :semy-layout}}
   :rectangular? {:type :boolean
                  :default false
                  :ui {:label {:en "Rectangular grid"
                               :de "Rechteckiges Gitter"}}}
   :manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}})

(defn options [data]
  (-> default-options
      (update :origin (fn [origin]
                        (when origin
                          (position/adjust-options origin (-> data :origin)))))))

(defmethod interface/component-options :heraldry.component/semy [context]
  (options (interface/get-raw-data context)))
