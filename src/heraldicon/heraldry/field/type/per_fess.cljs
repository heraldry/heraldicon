(ns heraldicon.heraldry.field.type.per-fess
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/per-fess)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-fess)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod field.interface/options field-type [context]
  {:anchor {:point {:type :choice
                    :choices (position/anchor-choices
                              [:fess
                               :chief
                               :base
                               :honour
                               :nombril
                               :top
                               :center
                               :bottom])
                    :default :fess
                    :ui {:label :string.option/point}}
            :offset-y {:type :range
                       :min -45
                       :max 45
                       :default 0
                       :ui {:label :string.option/offset-y
                            :step 0.1}}
            :ui {:label :string.option/anchor
                 :form-type :position}}
   :line (line/options (c/++ context :line))})

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        anchor-point (position/calculate anchor environment :fess)
        top-left (:top-left points)
        real-left (assoc (:left points) :y (:y anchor-point))
        real-right (assoc (:right points) :y (:y anchor-point))
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        left (v/sub real-left (v/Vector. required-extra-length 0))
        right (v/add real-right (v/Vector. required-extra-length 0))
        bottom-right (:bottom-right points)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         left right
                                         :context context
                                         :environment environment)
        parts [[["M" (v/add left
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/add right
                                        line-one-end)
                                 (v/add left
                                        line-one-start)])
                 "z"]
                [top-left
                 real-right]]

               [["M" (v/add left
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:right :left]
                                [(v/add right
                                        line-one-end)
                                 (v/add left
                                        line-one-start)])
                 "z"]
                [real-left
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil]
      environment]
     [line/render line [line-one-data] left outline? context]]))
