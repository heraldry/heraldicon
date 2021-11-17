(ns heraldry.render-options
  (:require
   [heraldry.coat-of-arms.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.texture :as texture]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(defmethod interface/options-subscriptions :heraldry.component/render-options [_context]
  #{[:mode]
    [:texture]})

(defmethod interface/options :heraldry.component/render-options [context]
  (let [mode (-> context (c/++ :mode) interface/get-raw-data (or :colours))
        texture (-> context (c/++ :texture) interface/get-raw-data (or :none))
        escutcheon-option (if (-> context :path (= [:collection-form :render-options]))
                            {:type :choice
                             :choices escutcheon/choices
                             :default :none
                             :ui {:label {:en "Escutcheon"
                                          :de "Schild"}
                                  :form-type :escutcheon-select}}
                            {:type :choice
                             :choices (drop 1 escutcheon/choices)
                             :default :heater
                             :ui {:label strings/escutcheon
                                  :form-type :escutcheon-select}})]
    (cond-> {:escutcheon escutcheon-option

             :mode {:type :choice
                    :choices [[{:en "Colours"
                                :de "Farben"} :colours]
                              [{:en "Hatching"
                                :de "Schraffur"} :hatching]]
                    :default :colours
                    :ui {:label strings/mode
                         :form-type :radio-select}}

             :texture {:type :choice
                       :choices texture/choices
                       :default :none
                       :ui {:label {:en "Texture"
                                    :de "Textur"}}}

             :shiny? {:type :boolean
                      :default false
                      :ui {:label {:en "Shiny"
                                   :de "Glanz"}}}

             :escutcheon-shadow? {:type :boolean
                                  :default false
                                  :ui {:label {:en "Escutcheon shadow (ignored for export)"
                                               :de "Schildschatten (beim Export ignoriert)"}}}

             :escutcheon-outline? {:type :boolean
                                   :default false
                                   :ui {:label {:en "Escutcheon outline"
                                                :de "Schildumrandung"}}}

             :outline? {:type :boolean
                        :default false
                        :ui {:label {:en "Draw outline"
                                     :de "Zeichne Umrandung"}}}

             :squiggly? {:type :boolean
                         :default false
                         :ui {:label {:en "Squiggly lines (can be slow)"
                                      :de "Schörkelige Linien (kann langsam sein)"}}}

             :preview-original? {:type :boolean
                                 :ui {:label {:en "Preview original (don't replace colours)"
                                              :de "Original anzeigen (Farben nicht ersetzen)"}}}
             :coat-of-arms-angle {:type :range
                                  :default 0
                                  :min -45
                                  :max 45
                                  :ui {:label {:en "Shield angle"
                                               :de "Schildwinkel"}
                                       :additional-values [[{:en "Half"
                                                             :de "Halb"} 22.5]
                                                           ["2/3" 30]
                                                           [{:en "Full"
                                                             :de "Voll"} 45]]
                                       :step 1}}
             :scope {:type :choice
                     :choices [[{:en "Everything (Helms, etc.)"
                                 :de "Vollwappen (Helme, etc.)"} :achievement]
                               [{:en "Coat of Arms with Helm"
                                 :de "Wappen mit Helmzier"} :coat-of-arms-and-helm]
                               [strings/coat-of-arms :coat-of-arms]]
                     :default :achievement
                     :ui {:label {:en "Scope"
                                  :de "Umfang"}}}}

      (not= texture :none) (assoc :texture-displacement?
                                  {:type :boolean
                                   :default false
                                   :ui {:label {:en "Simulate surface"
                                                :de "Oberfläche simulieren"}}})

      (= mode :colours) (assoc :theme {:type :choice
                                       :choices tincture/theme-choices
                                       :default tincture/default-theme
                                       :ui {:label {:en "Theme"
                                                    :de "Farbschema"}
                                            :form-type :theme-select}}))))
