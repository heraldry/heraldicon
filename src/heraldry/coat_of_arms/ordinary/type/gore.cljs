(ns heraldry.coat-of-arms.ordinary.type.gore
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]))

(defn arm-diagonal [origin-point anchor-point]
  (-> (v/sub anchor-point origin-point)
      v/normal
      (v/mul 200)))

(def ordinary-type :heraldry.ordinary.type/gore)

(defmethod ordinary-interface/display-name ordinary-type [_] "Gore")

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line))
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :choice
                             :choices [[strings/top-left :top-left]
                                       [strings/top-right :top-right]
                                       [strings/angle :angle]]
                             :default :top-left
                             :ui {:label strings/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point {:type :choice
                      :choices [[strings/fess-point :fess]
                                [strings/chief-point :chief]
                                [strings/base-point :base]]
                      :default :fess
                      :ui {:label strings/point}}
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
                                         :min -80
                                         :max 80
                                         :default -45
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
     :line line-style
     :opposite-line opposite-line-style
     :outline? options/plain-outline?-option}))

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))

        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        left? (case (-> anchor :point)
                :top-left true
                :angle (-> anchor :angle neg?)
                false)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     -90)
        bottom (:bottom points)
        relative-arm (arm-diagonal origin-point anchor-point)
        diagonal-top (v/add origin-point relative-arm)
        [_ intersection-top] (v/environment-intersections origin-point diagonal-top environment)
        flipped? (not left?)
        {line-diagonal :line
         line-diagonal-start :line-start
         :as line-diagonal-data} (line/create line
                                              origin-point diagonal-top
                                              :real-start 0
                                              :real-end (-> (v/sub intersection-top origin-point)
                                                            v/abs)
                                              :flipped? flipped?
                                              :reversed? true
                                              :context context
                                              :environment environment)
        {line-down :line
         line-down-end :line-end
         :as line-down-data} (line/create opposite-line
                                          origin-point bottom
                                          :flipped? flipped?
                                          :real-start 0
                                          :real-end (-> (v/sub bottom origin-point)
                                                        v/abs)
                                          :context context
                                          :environment environment)
        part [["M" (v/add diagonal-top
                          line-diagonal-start)
               (path/stitch line-diagonal)
               "L" origin-point
               (path/stitch line-down)
               (infinity/path (if left?
                                :clockwise
                                :counter-clockwise)
                              [:bottom :top]
                              [(v/add bottom
                                      line-down-end)
                               (v/add diagonal-top
                                      line-diagonal-start)])
               "z"]
              [(if left?
                 top-left
                 top-right)
               origin-point
               bottom]]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     [line/render line [line-diagonal-data line-down-data] diagonal-top outline? context]]))
