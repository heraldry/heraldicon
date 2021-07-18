(ns heraldry.render-options
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.ui.interface :as interface]))

(def mode-choices
  [["Colours" :colours]
   ["Hatching" :hatching]])

(def default-options
  {:escutcheon-override {:type :choice
                         :choices escutcheon/choices
                         :default :none
                         :ui {:label "Escutcheon Override"
                              :form-type :escutcheon-select}}

   :mode {:type :choice
          :choices mode-choices
          :default :colours
          :ui {:label "Mode"
               :form-type :radio-select}}

   :theme {:type :choice
           :choices tincture/theme-choices
           :default tincture/default-theme
           :ui {:label "Theme"
                :form-type :theme-select}}

   :texture {:type :choice
             :choices texture/choices
             :default :none
             :ui {:label "Texture"}}

   :texture-displacement? {:type :boolean
                           :default false
                           :ui {:label "Apply texture"}}

   :shiny? {:type :boolean
            :default false
            :ui {:label "Shiny"}}

   :escutcheon-shadow? {:type :boolean
                        :default false
                        :ui {:label "Escutcheon shadow (ignored for export)"}}

   :escutcheon-outline? {:type :boolean
                         :default false
                         :ui {:label "Escutcheon outline"}}

   :outline? {:type :boolean
              :default false
              :ui {:label "Draw outline"}}

   :squiggly? {:type :boolean
               :default false
               :ui {:label "Squiggly lines (can be slow)"}}

   :preview-original? {:type :boolean
                       :ui {:label "Preview original (don't replace colours)"}}})

(defn options [render-options]
  (when render-options
    (let [{:keys [mode texture]} (options/sanitize render-options default-options)]
      (cond-> default-options
        (= texture :none) (dissoc :texture-displacement?)
        (not= mode :colours) (dissoc :theme)))))

(defmethod interface/component-options :render-options [data _path]
  (options data))

;; TODO: this is a crutch, because this logic can't live in tincture/pick due to a cyclic dependency
(defn pick-tincture [tincture render-options]
  (let [[mode theme] (options/effective-values [[:mode] [:theme]] render-options options)]
    (tincture/pick tincture {:mode mode
                             :theme theme})))
