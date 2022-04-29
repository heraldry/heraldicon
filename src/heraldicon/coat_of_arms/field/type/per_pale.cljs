(ns heraldicon.coat-of-arms.field.type.per-pale
  (:require
   [heraldicon.coat-of-arms.field.interface :as field.interface]
   [heraldicon.coat-of-arms.field.shared :as shared]
   [heraldicon.coat-of-arms.infinity :as infinity]
   [heraldicon.coat-of-arms.line.core :as line]
   [heraldicon.coat-of-arms.position :as position]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/per-pale)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-pale)

(defmethod field.interface/part-names field-type [_] ["dexter" "sinister"])

(defmethod interface/options field-type [context]
  {:anchor {:point {:type :choice
                    :choices [[:string.option.point-choice/fess :fess]
                              [:string.option.point-choice/dexter :dexter]
                              [:string.option.point-choice/sinister :sinister]
                              [:string.option.point-choice/left :left]
                              [:string.option.point-choice/right :right]]
                    :default :fess
                    :ui {:label :string.option/point}}
            :offset-x {:type :range
                       :min -45
                       :max 45
                       :default 0
                       :ui {:label :string.option/offset-x
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
        real-top (assoc (:top points) :x (:x anchor-point))
        real-bottom (assoc (:bottom points) :x (:x anchor-point))
        bottom-right (:bottom-right points)
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        top (v/sub real-top (v/v 0 required-extra-length))
        bottom (v/add real-bottom (v/v 0 required-extra-length))
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         top
                                         bottom
                                         :context context
                                         :environment environment)

        parts [[["M" (v/add top
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:bottom :top]
                                [(v/add bottom
                                        line-one-end)
                                 (v/add top
                                        line-one-start)])
                 "z"]
                [top-left
                 real-bottom]]

               [["M" (v/add top
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:bottom :top]
                                [(v/add bottom
                                        line-one-end)
                                 (v/add top
                                        line-one-start)])
                 "z"]
                [real-top
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil]
      environment]
     [line/render line [line-one-data] top outline? context]]))
