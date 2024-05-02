(ns heraldicon.heraldry.ordinary.type.bordure
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/bordure)

(defn override-offset-default [line-type line-options]
  (if (:offset line-options)
    (cond
      (#{:wavy
         :indented
         :dancetty
         :nebuly
         :urdy
         :fir-tree-topped
         :fir-twigged
         :rayonny-flaming} line-type) (assoc-in line-options [:offset :default] 0.25)
      (#{:flory-counter-flory} line-type) (assoc-in line-options [:offset :default] -0.25)
      (#{:invected
         :engrailed
         :urdy
         :wolf-toothed} line-type) (assoc-in line-options [:offset :default] 0.5)
      (:spacing line-options) (assoc-in line-options [:offset :default] -0.5)
      :else line-options)
    line-options))

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bordure)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-type (or (interface/get-raw-data (c/++ context :line :type))
                      :straight)
        line-options (override-offset-default
                      line-type
                      (line/options (c/++ context :line)
                                    :corner-damping? true))]
    {:adapt-to-ordinaries? {:type :option.type/boolean
                            :default false
                            :ui/label :string.option/adapt-to-ordinaries?}
     :thickness {:type :option.type/range
                 :min 0.1
                 :max 35
                 :default 12
                 :ui/label :string.option/thickness
                 :ui/step 0.1}
     :corner-radius {:type :option.type/range
                     :min 0
                     :max 20
                     :default (case line-type
                                :straight 0
                                5)
                     :ui/label :string.option/corner-radius
                     :ui/step 0.1}
     :smoothing {:type :option.type/range
                 :min 0
                 :max 20
                 :default 0
                 :ui/label :string.option/smoothing
                 :ui/tooltip :string.tooltip/smoothing
                 :ui/step 0.1}
     :line (-> line-options
               (dissoc :flipped?)
               (update-in [:type :choices]
                          (fn [choices]
                            (into []
                                  (remove (fn [[_ line-type]]
                                            (and (not= line-type :straight)
                                                 (-> line-type line/kinds-pattern-map :full?))))
                                  choices))))
     :outline? options/plain-outline?-option}))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]} (interface/get-parent-field-environment context)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        corner-radius (interface/get-sanitized-data (c/++ context :corner-radius))
        smoothing (interface/get-sanitized-data (c/++ context :smoothing))
        percentage-base (min width height)
        parent-shape (interface/get-parent-field-shape context)
        ;; TODO: calculate the actual line length?
        line-length percentage-base
        thickness (math/percent-of percentage-base thickness)
        edge (-> parent-shape
                 (environment/shrink-shape thickness :round)
                 (path/round-corners corner-radius smoothing))]
    (post-process/properties
     {:type ordinary-type
      :edge edge
      :line-length line-length
      :percentage-base percentage-base}
     context)))

(defmethod interface/environment ordinary-type [context]
  (interface/get-parent-field-environment context))

(defmethod interface/render-shape ordinary-type [context]
  (let [{:keys [edge line]} (interface/get-properties context)
        parent-environment (interface/get-parent-field-environment context)
        big-shape (shape/build-shape context :full)
        shape (cond-> edge
                (not= (:type line) :straight) (line/modify-path line parent-environment))]
    {:shape [big-shape shape]
     :edges [{:paths [shape]}]}))

(defmethod cottising/cottise-properties ordinary-type [_context _properties]
  nil)

(defmethod interface/parent-field-environment ordinary-type [context]
  (interface/get-environment (interface/parent context)))

(prefer-method interface/parent-field-environment ordinary-type :heraldry/ordinary)

(defmethod interface/parent-field-shape ordinary-type [context]
  (let [parent-context (interface/parent context)
        context-for-adapt-check (if (isa? (interface/effective-component-type context) :heraldry/subfield)
                                  parent-context
                                  context)]
    (if (interface/get-sanitized-data (c/++ context-for-adapt-check :adapt-to-ordinaries?))
      (interface/get-exact-impacted-shape parent-context)
      (interface/get-exact-shape parent-context))))

(prefer-method interface/parent-field-shape ordinary-type :heraldry/ordinary)
