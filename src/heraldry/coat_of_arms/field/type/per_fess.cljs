(ns heraldry.coat-of-arms.field.type.per-fess
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
   [heraldry.options :as options]
   [heraldry.strings :as strings]))

(def field-type :heraldry.field.type/per-fess)

(defmethod field-interface/display-name field-type [_] {:en "Per fess"
                                                        :de "Geteilt"})

(defmethod field-interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/options field-type [context]
  (let [line-data (interface/get-raw-data (c/++ context :line))
        line-style (line/options line-data)]
    {:origin {:point {:type :choice
                      :choices [[strings/fess-point :fess]
                                [strings/chief-point :chief]
                                [strings/base-point :base]
                                [strings/honour-point :honour]
                                [strings/nombril-point :nombril]
                                [strings/top :top]
                                [strings/bottom :bottom]]
                      :default :fess
                      :ui {:label strings/point}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-y
                              :step 0.1}}
              :ui {:label strings/origin
                   :form-type :position}}
     :line line-style
     :outline? options/plain-outline?-option}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        real-left (assoc (:left points) :y (:y origin-point))
        real-right (assoc (:right points) :y (:y origin-point))
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
