(ns heraldry.render-options
  (:require
   [heraldry.coat-of-arms.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.texture :as texture]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/render-options [_context]
  #{[:escutcheon]
    [:mode]
    [:texture]})

(defmethod interface/options :heraldry.component/render-options [context]
  (let [mode (-> context (c/++ :mode) interface/get-raw-data (or :colours))
        texture (-> context (c/++ :texture) interface/get-raw-data (or :none))
        escutcheon-option (if (-> context :path (= [:collection-form :render-options]))
                            {:type :choice
                             :choices escutcheon/choices
                             :default :none
                             :ui {:label (string "Escutcheon")
                                  :form-type :escutcheon-select}}
                            {:type :choice
                             :choices (drop 1 escutcheon/choices)
                             :default :heater
                             :ui {:label (string "Escutcheon")
                                  :form-type :escutcheon-select}})
        escutcheon (-> context (c/++ :escutcheon) interface/get-raw-data
                       (or (-> escutcheon-option :choices first second)))]
    (cond-> {:escutcheon escutcheon-option

             :mode {:type :choice
                    :choices [[(string "Colours") :colours]
                              [(string "Hatching") :hatching]]
                    :default :colours
                    :ui {:label (string "Mode")
                         :form-type :radio-select}}

             :texture {:type :choice
                       :choices texture/choices
                       :default :none
                       :ui {:label (string "Texture")}}

             :shiny? {:type :boolean
                      :default false
                      :ui {:label (string "Shiny")}}

             :escutcheon-shadow? {:type :boolean
                                  :default false
                                  :ui {:label (string "Escutcheon shadow (ignored for export)")}}

             :escutcheon-outline? {:type :boolean
                                   :default false
                                   :ui {:label (string "Escutcheon outline")}}

             :outline? {:type :boolean
                        :default false
                        :ui {:label (string "Draw outline")}}

             :squiggly? {:type :boolean
                         :default false
                         :ui {:label (string "Squiggly lines (can be slow)")}}

             :preview-original? {:type :boolean
                                 :ui {:label (string "Preview original (don't replace colours)")}}
             :coat-of-arms-angle {:type :range
                                  :default 0
                                  :min -45
                                  :max 45
                                  :ui {:label (string "Shield angle")
                                       :additional-values [[(string "Half") 22.5]
                                                           [(string "2/3") 30]
                                                           [(string "Full") 45]]
                                       :step 1}}
             :scope {:type :choice
                     :choices [[(string "Everything (Helms, etc.)") :achievement]
                               [(string "Coat of Arms with Helm") :coat-of-arms-and-helm]
                               [(string "Coat of Arms") :coat-of-arms]]
                     :default :achievement
                     :ui {:label (string "Scope")}}}

      (= escutcheon :flag) (merge escutcheon/flag-options)

      (not= texture :none) (assoc :texture-displacement?
                                  {:type :boolean
                                   :default false
                                   :ui {:label (string "Simulate surface")}})

      (= mode :colours) (assoc :theme {:type :choice
                                       :choices tincture/theme-choices
                                       :default tincture/default-theme
                                       :ui {:label (string "Theme")
                                            :form-type :theme-select}}))))
