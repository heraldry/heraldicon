(ns heraldry.coat-of-arms.field.type.per-pale
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(def field-type :heraldry.field.type/per-pale)

(defmethod field-interface/display-name field-type [_] {:en "Per pale"
                                                        :de "Gespalten"})

(defmethod field-interface/part-names field-type [_] ["dexter" "sinister"])

(defmethod interface/options-subscriptions field-type [context]
  (-> #{[:origin :point]
        [:anchor :point]}
      (into (line/options-subscriptions (c/++ context :line)))))

(defmethod interface/options field-type [context]
  {:origin {:point {:type :choice
                    :choices [[strings/fess-point :fess]
                              [strings/dexter-point :dexter]
                              [strings/sinister-point :sinister]
                              [strings/left :left]
                              [strings/right :right]]
                    :default :fess
                    :ui {:label strings/point}}
            :offset-x {:type :range
                       :min -45
                       :max 45
                       :default 0
                       :ui {:label strings/offset-x
                            :step 0.1}}
            :ui {:label strings/origin
                 :form-type :position}}
   :line (line/options (c/++ context :line))})

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        real-top (assoc (:top points) :x (:x origin-point))
        real-bottom (assoc (:bottom points) :x (:x origin-point))
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
