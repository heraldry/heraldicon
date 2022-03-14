(ns heraldry.coat-of-arms.geometry)

(def default-options
  {:size {:type :range
          :min 5
          :max 250
          :default 50
          :ui {:label :string.option/size
               :step 0.1}}
   :stretch {:type :range
             :min 0.33
             :max 3
             :default 1
             :ui {:label :string.option/stretch
                  :step 0.01}}
   :mirrored? {:type :boolean
               :default false
               :ui {:label :string.option/mirrored?}}
   :reversed? {:type :boolean
               :default false
               :ui {:label :string.option/reversed?}}
   :ui {:label :string.option/geometry
        :form-type :geometry}})
