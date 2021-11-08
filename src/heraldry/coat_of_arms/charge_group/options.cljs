(ns heraldry.coat-of-arms.charge-group.options
  (:require
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def type-choices
  [[{:en "Rows"
     :de "Zeilen"} :heraldry.charge-group.type/rows]
   [{:en "Columns"
     :de "Spalten"} :heraldry.charge-group.type/columns]
   [{:en "Arc"
     :de "Bogen"} :heraldry.charge-group.type/arc]])

(def type-map
  (util/choices->map type-choices))

(def type-option
  {:type :choice
   :choices type-choices
   :ui {:label strings/type
        :form-type :charge-group-type-select}})

(def shared-options
  {:origin (-> position/default-options
               (dissoc :alignment))
   :manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}})

(defn post-process [options context]
  (-> options
      (update :origin (fn [position]
                        (when position
                          (-> position
                              (position/adjust-options (interface/get-raw-data (c/++ context :origin)))))))))

(defn rows-or-columns [context]
  (-> shared-options
      (merge {:spacing {:type :range
                        :min 1
                        :max 100
                        :default 40
                        :ui {:label strings/spacing
                             :step 0.1}}
              :stretch {:type :range
                        :min 0
                        :max 5
                        :default 1
                        :ui {:label strings/stretch
                             :step 0.01}}
              :strip-angle {:type :range
                            :min -90
                            :max 90
                            :default 0
                            :ui {:label {:en "Strip angle"
                                         :de "Streifenwinkel"}
                                 :step 1}}})
      (post-process context)))

(defmethod interface/options :heraldry.charge-group.type/rows [context]
  (rows-or-columns context))

(defmethod interface/options :heraldry.charge-group.type/columns [context]
  (rows-or-columns context))

(defmethod interface/options :heraldry.charge-group.type/arc [context]
  (-> shared-options
      (merge {:start-angle {:type :range
                            :min -180
                            :max 180
                            :default 0
                            :ui {:label {:en "Start angle"
                                         :de "Startwinkel"}
                                 :step 1}}
              :arc-angle {:type :range
                          :min 0
                          :max 360
                          :default 360
                          :ui {:label {:en "Arc angle"
                                       :de "Bogenwinkel"}
                               :step 1}}
              :slots {:type :range
                      :min 1
                      :max 20
                      :default 5
                      :integer? true
                      :ui {:label strings/number
                           :form-type :charge-group-slot-number}}
              :radius {:type :range
                       :min 0
                       :max 100
                       :default 30
                       :ui {:label strings/radius
                            :step 0.1}}
              :arc-stretch {:type :range
                            :min 0
                            :max 5
                            :default 1
                            :ui {:label strings/stretch
                                 :step 0.01}}
              :rotate-charges? {:type :boolean
                                :default false
                                :ui {:label {:en "Rotate charges"
                                             :de "Wappenfiguren rotieren"}}}})
      (post-process context)))

(defmethod interface/options-dispatch-fn :heraldry.component/charge-group [context]
  (interface/get-raw-data (c/++ context :type)))

(defmethod interface/component-options :heraldry.component/charge-group [context]
  (-> (interface/options context)
      (assoc :type type-option)))

(defmethod interface/component-options :heraldry.component/charge-group-strip [_context]
  {:slots {:type :range
           :min 0
           :max 10
           :default 3
           :integer? true
           :ui {:label strings/number
                :form-type :charge-group-slot-number}}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1
             :ui {:label strings/stretch
                  :step 0.01}}
   :offset {:type :range
            :min -3
            :max 3
            :default 0
            :ui {:label strings/offset
                 :step 0.01}}})
