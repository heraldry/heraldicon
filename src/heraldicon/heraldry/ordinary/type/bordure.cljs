(ns heraldicon.heraldry.ordinary.type.bordure
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/bordure)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bordure)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-type (or (interface/get-raw-data (c/++ context :line :type))
                      :straight)]
    {:thickness {:type :range
                 :min 0.1
                 :max 35
                 :default 12
                 :ui {:label :string.option/thickness
                      :step 0.1}}
     :corner-radius {:type :range
                     :min 0
                     :max 20
                     :default (case line-type
                                :straight 0
                                5)
                     :ui {:label :string.option/corner-radius
                          :step 0.1}}
     :smoothing {:type :range
                 :min 0
                 :max 20
                 :default 0
                 :ui {:label :string.option/smoothing
                      :tooltip :string.tooltip/smoothing
                      :step 0.1}}
     :line (-> (line/options (c/++ context :line)
                             :fimbriation? false
                             :corner-dampening? true)
               (dissoc :flipped?)
               (update-in [:type :choices]
                          (fn [choices]
                            (into []
                                  (remove (fn [[_ line-type]]
                                            (and (not= line-type :straight)
                                                 (-> line-type line/kinds-pattern-map :full?))))
                                  choices))))
     :outline? options/plain-outline?-option}))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothing (interface/get-sanitized-data (c/++ context :smoothing))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        line-type (interface/get-sanitized-data (c/++ context :line :type))
        points (:points environment)
        width (:width environment)
        thickness (math/percent-of width thickness)
        environment-shape (-> environment
                              (update-in [:shape :paths] (partial take 1))
                              environment/effective-shape)
        bordure-shape (environment/shrink-shape environment-shape thickness :round)
        bordure-shape (cond-> (path/round-corners bordure-shape corner-radius smoothing)
                        (not= line-type :straight) (line/modify-path (interface/get-sanitized-data
                                                                      (c/++ context :line))
                                                                     environment))
        part [{:paths [environment-shape
                       bordure-shape]}
              [(:top-left points)
               (:bottom-right points)]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (when outline?
       [:g (outline/style context)
        [:path {:d bordure-shape}]])]))
