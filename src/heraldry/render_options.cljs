(ns heraldry.render-options
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
            [heraldry.options :as options]))

(def default-options
  {:escutcheon {:type :choice
                :choices (drop 1 escutcheon/choices)
                :default :heater
                :ui {:label "Escutcheon"
                     :form-type :escutcheon-select}}

   :mode {:type :choice
          :choices [["Colours" :colours]
                    ["Hatching" :hatching]]
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
                       :ui {:label {:en "Preview original (don't replace colours)"
                                    :de "Preview Original (Farben nicht ersetzen)"}}}
   :coat-of-arms-angle {:type :range
                        :default 0
                        :min -45
                        :max 45
                        :ui {:label "Shield angle"
                             :additional-values [["Half" 22.5]
                                                 ["2/3rds" 30]
                                                 ["Full" 45]]
                             :step 1}}
   :scope {:type :choice
           :choices [["Everything (Helms, etc.)" :achievement]
                     ["Coat of Arms" :coat-of-arms]]
           :default :achievement
           :ui {:label "Scope"}}})

(defn options [render-options]
  (let [{:keys [mode texture]} (options/sanitize render-options default-options)]
    (cond-> default-options
      (= texture :none) (dissoc :texture-displacement?)
      (not= mode :colours) (dissoc :theme))))

(defmethod interface/component-options :heraldry.component/render-options [path data]
  (cond-> (options data)
    (= path [:collection-form :render-options]) (assoc :escutcheon {:type :choice
                                                                    :choices escutcheon/choices
                                                                    :default :none
                                                                    :ui {:label "Escutcheon"
                                                                         :form-type :escutcheon-select}})))
