(ns heraldry.coat-of-arms.field.type.per-bend-sinister
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]))

(def field-type :heraldry.field.type/per-bend-sinister)

(defmethod field-interface/display-name field-type [_] (string "Per bend sinister"))

(defmethod field-interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line))
        origin-point-option {:type :choice
                             :choices [[strings/fess-point :fess]
                                       [strings/chief-point :chief]
                                       [strings/base-point :base]
                                       [strings/honour-point :honour]
                                       [strings/nombril-point :nombril]
                                       [strings/top-right :top-right]
                                       [strings/bottom-left :bottom-left]]
                             :default :top-right
                             :ui {:label strings/point}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        anchor-point-option {:type :choice
                             :choices (case current-origin-point
                                        :top-right [[strings/fess-point :fess]
                                                    [strings/chief-point :chief]
                                                    [strings/base-point :base]
                                                    [strings/honour-point :honour]
                                                    [strings/nombril-point :nombril]
                                                    [strings/bottom-left :bottom-left]
                                                    [strings/angle :angle]]
                                        :bottom-left [[strings/fess-point :fess]
                                                      [strings/chief-point :chief]
                                                      [strings/base-point :base]
                                                      [strings/honour-point :honour]
                                                      [strings/nombril-point :nombril]
                                                      [strings/top-right :top-right]
                                                      [strings/angle :angle]]
                                        [[strings/top-right :top-right]
                                         [strings/bottom-left :bottom-left]
                                         [strings/angle :angle]])
                             :default (case current-origin-point
                                        :top-right :fess
                                        :bottom-left :fess
                                        :top-right)
                             :ui {:label strings/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point origin-point-option
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label strings/offset-y
                              :step 0.1}}
              :ui {:label strings/origin
                   :form-type :position}}
     :anchor (cond-> {:point anchor-point-option
                      :ui {:label strings/anchor
                           :form-type :position}}

               (= current-anchor-point
                  :angle) (assoc :angle {:type :range
                                         :min 0
                                         :max 360
                                         :default 45
                                         :ui {:label strings/angle}})

               (not= current-anchor-point
                     :angle) (assoc :offset-x {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label strings/offset-x
                                                    :step 0.1}}
                                    :offset-y {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label strings/offset-y
                                                    :step 0.1}}))
     :line line-style}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))

        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        direction (v/sub anchor-point origin-point)
        direction (v/normal (v/v (-> direction :x Math/abs)
                                 (-> direction :y Math/abs -)))
        initial-diagonal-start (-> direction
                                   (v/mul -1000)
                                   (v/add origin-point))
        initial-diagonal-end (-> direction
                                 (v/mul 1000)
                                 (v/add origin-point))
        [real-diagonal-start
         real-diagonal-end] (v/environment-intersections
                             initial-diagonal-start
                             initial-diagonal-end
                             environment)
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        extra-dir (-> direction
                      (v/mul required-extra-length))
        diagonal-start (v/sub real-diagonal-start extra-dir)
        diagonal-end (v/add real-diagonal-end extra-dir)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         diagonal-start diagonal-end
                                         :context context
                                         :environment environment)
        parts [[["M" (v/add diagonal-start
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:top :left]
                                [(v/add diagonal-end
                                        line-one-end)
                                 (v/add diagonal-start
                                        line-one-start)])
                 "z"]
                [real-diagonal-start
                 top-left
                 real-diagonal-end]]

               [["M" (v/add diagonal-start
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:top :left]
                                [(v/add diagonal-end
                                        line-one-end)
                                 (v/add diagonal-start
                                        line-one-start)])
                 "z"]
                [real-diagonal-start
                 bottom-right
                 real-diagonal-end]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil]
      environment]
     [line/render line [line-one-data] diagonal-start outline? context]]))
