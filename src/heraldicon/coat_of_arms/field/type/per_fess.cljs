(ns heraldicon.coat-of-arms.field.type.per-fess
  (:require
   [heraldicon.coat-of-arms.field.interface :as field.interface]
   [heraldicon.coat-of-arms.field.shared :as shared]
   [heraldicon.coat-of-arms.infinity :as infinity]
   [heraldicon.coat-of-arms.line.core :as line]
   [heraldicon.coat-of-arms.position :as position]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.svg.path :as path]
   [heraldicon.math.vector :as v]))

(def field-type :heraldry.field.type/per-fess)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-fess)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/options field-type [context]
  {:anchor {:point {:type :choice
                    :choices [[:string.option.point-choice/fess :fess]
                              [:string.option.point-choice/chief :chief]
                              [:string.option.point-choice/base :base]
                              [:string.option.point-choice/honour :honour]
                              [:string.option.point-choice/nombril :nombril]
                              [:string.option.point-choice/top :top]
                              [:string.option.point-choice/bottom :bottom]]
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
        left (v/sub real-left (v/v required-extra-length 0))
        right (v/add real-right (v/v required-extra-length 0))
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
