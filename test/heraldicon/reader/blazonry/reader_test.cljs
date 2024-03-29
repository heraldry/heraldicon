(ns heraldicon.reader.blazonry.reader-test
  (:require
   [cljs.test :refer-macros [are deftest]]
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.reader :as reader]
   [heraldicon.reader.blazonry.result :as result]))

(deftest parsing
  (are [blazon form] (= (reader/read blazon parser/default) form)

    "or"
    {:type :heraldry.field.type/plain
     :tincture :or}

    "tenné"
    {:type :heraldry.field.type/plain
     :tincture :tenne}

    "erminois"
    {:type :heraldry.field.type/plain
     :tincture :erminois}

    "or a fess gules, pale sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or a pale sable cottised wavy azure and gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :cottising {:cottise-1 {:type :heraldry/cottise
                                           :field {:type :heraldry.field.type/plain
                                                   :tincture :azure}
                                           :line {:type :wavy}}
                               :cottise-2 {:type :heraldry/cottise
                                           :field {:type :heraldry.field.type/plain
                                                   :tincture :gules}}
                               :cottise-opposite-1 {:type :heraldry/cottise
                                                    :field {:type :heraldry.field.type/plain
                                                            :tincture :azure}
                                                    :line {:type :wavy}}
                               :cottise-opposite-2 {:type :heraldry/cottise
                                                    :field {:type :heraldry.field.type/plain
                                                            :tincture :gules}}}}]}

    "or a pall sable cottised wavy azure and gules, cottices straight and indented argent, with doubly cottising urdy vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pall
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :cottising {:cottise-1 {:type :heraldry/cottise
                                           :field {:type :heraldry.field.type/plain
                                                   :tincture :azure}
                                           :line {:type :wavy}}
                               :cottise-2 {:type :heraldry/cottise
                                           :field {:type :heraldry.field.type/plain
                                                   :tincture :gules}}
                               :cottise-opposite-1 {:type :heraldry/cottise
                                                    :field {:type :heraldry.field.type/plain
                                                            :tincture :argent}
                                                    :line {:type :straight}
                                                    :opposite-line {:type :indented}}

                               :cottise-extra-1 {:type :heraldry/cottise
                                                 :field {:type :heraldry.field.type/plain
                                                         :tincture :vert}
                                                 :line {:type :urdy}}
                               :cottise-extra-2 {:type :heraldry/cottise
                                                 :field {:type :heraldry.field.type/plain
                                                         :tincture :vert}
                                                 :line {:type :urdy}}}}]}

    "or five tressures sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/orle
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or three bars azure"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}
                  {:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}
                  {:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}]}

    "or a fess enhanced gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :anchor {:offset-y 12.5}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a fess dehanced gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :anchor {:offset-y -12.5}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a chevron reversed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/chevron
                   :origin {:point :chief}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a pile reversed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pile
                   :anchor {:point :bottom}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a pile throughout gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/pile
                   :geometry {:stretch 1}
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}}]}

    "or a fess indented or, pale straight and urdy fimbriated or and argent sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :line {:type :indented}
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :line {:type :straight}
                   :opposite-line {:type :urdy
                                   :fimbriation {:mode :double
                                                 :tincture-1 :or
                                                 :tincture-2 :argent}}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "per fess sable and ermine"
    {:type :heraldry.field.type/per-fess
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]}

    "per fess sable and ermine"
    {:type :heraldry.field.type/per-fess
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]}

    "per fess dancetty sable and ermine"
    {:type :heraldry.field.type/per-fess
     :line {:type :dancetty}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]}

    "per fess fimbriated or sable and ermine"
    {:type :heraldry.field.type/per-fess
     :line {:fimbriation {:mode :single
                          :tincture-1 :or}}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]}

    "per fess fimbriated or and vert sable and ermine"
    {:type :heraldry.field.type/per-fess
     :line {:fimbriation {:mode :double
                          :tincture-1 :or
                          :tincture-2 :vert}}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]}

    "tierced per pale sable and ermine and azure"
    {:type :heraldry.field.type/tierced-per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "tierced per pale indented sable and ermine and azure"
    {:type :heraldry.field.type/tierced-per-pale
     :line {:type :indented}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "tierced per pale indented and dovetailed fimbriated azure and gules sable and ermine and azure"
    {:type :heraldry.field.type/tierced-per-pale
     :line {:type :indented}
     :opposite-line {:type :dovetailed
                     :fimbriation {:mode :double
                                   :tincture-1 :azure
                                   :tincture-2 :gules}}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "or a fess humetty indented or, pale wavy voided sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :line {:type :indented}
                   :humetty {:humetty? true}
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :line {:type :wavy}
                   :voided {:voided? true}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or a fess or, a pale sable, a chief vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/chief
                   :field {:type :heraldry.field.type/plain
                           :tincture :vert}}]}

    "or a fess or, a pale (sable, a chief vert)"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable
                           :components [{:type :heraldry.ordinary.type/chief
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :vert}}]}}]}

    "or a fess or, a pale sable charged with a chief vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable
                           :components [{:type :heraldry.ordinary.type/chief
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :vert}}]}}]}

    "per chevron enhanced sable and ermine"
    {:type :heraldry.field.type/per-chevron
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]
     :anchor {:offset-y 15}
     :orientation {:point :angle
                   :angle 45}}

    "per fess dehanced sable and ermine"
    {:type :heraldry.field.type/per-fess
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}]
     :anchor {:offset-y -12.5}}

    "tierced per pall reversed sable and ermine and vert"
    {:type :heraldry.field.type/tierced-per-pall
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :vert}}]
     :origin {:point :bottom}}

    "per pile reversed sable and ermine and vert"
    {:type :heraldry.field.type/per-pile
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :ermine}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :vert}}]
     :anchor {:point :top}}

    "per pale azure and argent, a fess sable"
    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :argent}}]
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "paly or and azure"
    {:type :heraldry.field.type/paly
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}]}

    "paly of three or and azure"
    {:type :heraldry.field.type/paly
     :layout {:num-fields-x 3}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 0}]}

    "paly of 8 or and azure"
    {:type :heraldry.field.type/paly
     :layout {:num-fields-x 8}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}]}

    "chequy of 2 or and azure"
    {:type :heraldry.field.type/chequy
     :layout {:num-fields-x 2}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "chequy of 5x6 or, azure, gules, sable"
    {:type :heraldry.field.type/chequy
     :layout {:num-base-fields 4
              :num-fields-x 5
              :num-fields-y 6}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :gules}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :sable}}]}

    "lozengy 2 tracts or and azure"
    {:type :heraldry.field.type/lozengy
     :layout {:num-fields-y 2}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "vairy 2 x4 or and azure"
    {:type :heraldry.field.type/vairy
     :layout {:num-fields-x 2
              :num-fields-y 4}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "vairy ancien or and azure"
    {:type :heraldry.field.type/vairy
     :variant :ancien
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "potenty en-point or and azure"
    {:type :heraldry.field.type/potenty
     :variant :en-point
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "papellony of three times twenty-eight tracts or and azure"
    {:type :heraldry.field.type/papellony
     :layout {:num-fields-x 3
              :num-fields-y 28}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]}

    "or a label of five points gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/label
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :num-points 5}]}

    "or a label of five gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/label
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :num-points 5}]}

    "or a label of 10 points truncated dovetailed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/label
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :num-points 10
                   :variant :truncated
                   :geometry {:eccentricity 0.4}}]}

    "or a label fimbriated or and vert gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/label
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :fimbriation {:mode :double
                                 :tincture-1 :or
                                 :tincture-2 :vert}}]}

    "or a star sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/star
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}

    "or ten roundels purpure four 3 2 1"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:charges [{:type :heraldry.charge.type/roundel
                              :field {:type :heraldry.field.type/plain
                                      :tincture :purpure}}]
                   :type :heraldry.charge-group.type/rows
                   :anchor {:point :center}
                   :spacing 23.75
                   :strips [{:type :heraldry.charge-group.element.type/strip
                             :slots [0 0 0 0]}
                            {:type :heraldry.charge-group.element.type/strip
                             :slots [0 0 0]}
                            {:type :heraldry.charge-group.element.type/strip
                             :slots [0 0]}
                            {:type :heraldry.charge-group.element.type/strip
                             :slots [0]}]}]}

    "or a lion sable, langued argent armed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/lion
                   :variant {:id nil
                             :version nil}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :tincture {:langued :argent
                              :armed :gules}}]}

    "or a lion sable, langued and armed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/lion
                   :variant {:id nil
                             :version nil}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :tincture {:langued :gules
                              :armed :gules}}]}

    "or a lion sable, langued armed gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/lion
                   :variant {:id nil
                             :version nil}
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :tincture {:langued :gules
                              :armed :gules}}]}

    "per pale or and azure, a fess of the first, a pale of the second"
    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}]
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}]}

    "or, a fess azure, a pale of the field"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :or}}]}

    "per pale azure and or, a fess azure charged with a star of the field"
    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}]
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure
                           :components [{:type :heraldry.charge.type/star
                                         :field {:type :heraldry.field.type/per-pale
                                                 :fields [{:type :heraldry.subfield.type/field
                                                           :field {:type :heraldry.field.type/plain
                                                                   :tincture :azure}}
                                                          {:type :heraldry.subfield.type/field
                                                           :field {:type :heraldry.field.type/plain
                                                                   :tincture :or}}]}}]}}]}

    "per pale azure and or, on a fess azure two stars of the field"
    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}]
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure
                           :components [{:type :heraldry.charge-group.type/rows
                                         :charges [{:type :heraldry.charge.type/star
                                                    :field {:type :heraldry.field.type/per-pale
                                                            :fields [{:type :heraldry.subfield.type/field
                                                                      :field {:type :heraldry.field.type/plain
                                                                              :tincture :azure}}
                                                                     {:type :heraldry.subfield.type/field
                                                                      :field {:type :heraldry.field.type/plain
                                                                              :tincture :or}}]}}]
                                         :spacing 47.5
                                         :strips [{:type :heraldry.charge-group.element.type/strip
                                                   :slots [0
                                                           0]}]}]}}]}

    "or, a star sable, a fess of the same, a star azure, a pale of the same"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/fess
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.ordinary.type/pale
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}
                  {:type :heraldry.charge.type/star
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}
                  {:type :heraldry.charge.type/star
                   :field {:type :heraldry.field.type/plain
                           :tincture :azure}}]}

    "or semé star gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry/semy
                   :charge {:type :heraldry.charge.type/star
                            :field {:type :heraldry.field.type/plain
                                    :tincture :gules}}}]}

    "or semy of 10xfive stars sable"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry/semy
                   :charge {:type :heraldry.charge.type/star
                            :field {:type :heraldry.field.type/plain
                                    :tincture :sable}}
                   :layout {:num-fields-x 10
                            :num-fields-y 5}}]}

    "quartered i. and 4th or, 2nd and base-dexter azure"
    {:type :heraldry.field.type/quartered
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}]}

    "quartered i. ii. or, 2nd and base-dexter azure"
    {:type :heraldry.field.type/quartered
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 0}]
     ::result/warnings
     ["Field for partition mentioned more than once: II."]}

    "paly i. ii. or, 3rd and 5th azure"
    {:type :heraldry.field.type/paly
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 2}
              {:type :heraldry.subfield.type/reference
               :index 2}]}

    "quartered chief and i. or, i. and 19th azure"
    {:type :heraldry.field.type/quartered
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :void}}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}]
     ::result/warnings
     ["Field for partition missing: II."
      "Fields not found in partition: XIX., chief"
      "Field for partition mentioned more than once: I."]}

    "quarterly of 5x6 or and azure"
    {:type :heraldry.field.type/quarterly
     :layout {:num-fields-x 5
              :num-fields-y 6}
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :or}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :azure}}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}
              {:type :heraldry.subfield.type/reference
               :index 0}
              {:type :heraldry.subfield.type/reference
               :index 1}]}

    "per pale chief or, base azure"
    {:type :heraldry.field.type/per-pale
     :fields [{:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :void}}
              {:type :heraldry.subfield.type/field
               :field {:type :heraldry.field.type/plain
                       :tincture :void}}]
     ::result/warnings
     ["Fields for partition missing: I., II."
      "Fields not found in partition: base, chief"]}

    "or a bend fimbriated or and vert gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/bend
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :line {:fimbriation {:mode :double
                                        :tincture-1 :or
                                        :tincture-2 :vert}}}]}

    "or a bend gules fimbriated or and vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/bend
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :line {:fimbriation {:mode :double
                                        :tincture-1 :or
                                        :tincture-2 :vert}}}]}

    "or a label gules fimbriated or and vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/label
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :fimbriation {:mode :double
                                 :tincture-1 :or
                                 :tincture-2 :vert}}]}

    "or a roundel gules fimbriated or and vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :fimbriation {:mode :double
                                 :tincture-1 :or
                                 :tincture-2 :vert}}]}

    "or a roundel fimbriated or and vert gules"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :gules}
                   :fimbriation {:mode :double
                                 :tincture-1 :or
                                 :tincture-2 :vert}}]}

    "azure a roundel fimbriated or sable armed gules"
    {:type :heraldry.field.type/plain
     :tincture :azure
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :tincture {:armed :gules}
                   :fimbriation {:mode :single
                                 :tincture-1 :or}}]}

    "azure a roundel sable fimbriated or armed gules"
    {:type :heraldry.field.type/plain
     :tincture :azure
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :tincture {:armed :gules}
                   :fimbriation {:mode :single
                                 :tincture-1 :or}}]}

    "azure a roundel sable armed gules fimbriated or"
    {:type :heraldry.field.type/plain
     :tincture :azure
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}
                   :tincture {:armed :gules}
                   :fimbriation {:mode :single
                                 :tincture-1 :or}}]}

    "or a roundel (sable a chief vert)"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable
                           :components [{:type :heraldry.ordinary.type/chief
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :vert}}]}}]}

    "or a roundel sable charged with a chief vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable
                           :components [{:type :heraldry.ordinary.type/chief
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :vert}}]}}]}

    "or a roundel sable a chief vert"
    {:type :heraldry.field.type/plain
     :tincture :or
     :components [{:type :heraldry.ordinary.type/chief
                   :field {:type :heraldry.field.type/plain
                           :tincture :vert}}
                  {:type :heraldry.charge.type/roundel
                   :field {:type :heraldry.field.type/plain
                           :tincture :sable}}]}
;;
    ))
