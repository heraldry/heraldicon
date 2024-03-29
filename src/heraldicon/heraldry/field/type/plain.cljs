(ns heraldicon.heraldry.field.type.plain
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(def field-type :heraldry.field.type/plain)

(defmethod field.interface/display-name field-type [_] :string.field.type/plain)

(defmethod field.interface/part-names field-type [_] [])

(defmethod field.interface/options field-type [context]
  (let [tincture (interface/get-raw-data (c/++ context :tincture))]
    (cond-> {:tincture {:type :option.type/choice
                        :choices tincture/choices
                        :default :none
                        :ui/label :string.option/tincture
                        :ui/element :ui.element/tincture-select}}
      (tincture/furs tincture) (assoc :pattern-scaling {:type :option.type/range
                                                        :min 0.1
                                                        :max 3
                                                        :default 1
                                                        :ui/label :string.option/pattern-scaling
                                                        :ui/step 0.01}
                                      :pattern-rotation {:type :option.type/range
                                                         :min -180
                                                         :max 180
                                                         :default 0
                                                         :ui/label :string.option/pattern-rotation
                                                         :ui/step 0.01}
                                      :pattern-offset-x {:type :option.type/range
                                                         :min 0
                                                         :max 10
                                                         :default 0
                                                         :ui/label :string.option/pattern-offset-x
                                                         :ui/step 0.01}
                                      :pattern-offset-y {:type :option.type/range
                                                         :min 0
                                                         :max 10
                                                         :default 0
                                                         :ui/label :string.option/pattern-offset-y
                                                         :ui/step 0.01}))))

(defmethod interface/properties field-type [_context]
  {:type field-type})

(defmethod interface/subfield-environments field-type [_context])

(defmethod interface/subfield-render-shapes field-type [_context])
