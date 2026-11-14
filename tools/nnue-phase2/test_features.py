"""Independent symmetry and injectivity checks for the selected pawn-pair vocabulary."""
import unittest
import chess
import codec


class FeaturesTest(unittest.TestCase):
    def test_native_quantization_uses_promoted_product_without_false_half_tie(self):
        self.assertEqual(128,codec.quantize_value(0.5039215683937073,255))
        self.assertEqual(-128,codec.quantize_value(-0.5039215683937073,255))
        self.assertEqual(1,codec.quantize_value(.5,1))
        self.assertEqual(-1,codec.quantize_value(-.5,1))
    def test_exhaustive_pair_identity_and_view_symmetries(self):
        # Verify all feasible two-pawn placements/colours; kings stay off pawn ranks.
        observed=set()
        for a in range(8,56):
            for b in range(a+1,56):
                if abs(a%8-b%8)>1:continue
                for ca in (False,True):
                    for cb in (False,True):
                        board=chess.Board(None);board.set_piece_at(chess.E1,chess.Piece(chess.KING,True));board.set_piece_at(chess.E8,chess.Piece(chess.KING,False))
                        board.set_piece_at(a,chess.Piece(chess.PAWN,ca));board.set_piece_at(b,chess.Piece(chess.PAWN,cb))
                        fs=codec.features(board,True,2);pairs=[x for x in fs if x>=7680]
                        self.assertEqual(1,len(pairs));self.assertTrue(7680<=pairs[0]<12240)
                        self.assertNotIn(pairs[0],observed);observed.add(pairs[0])
                        self.assertEqual(fs,codec.features(board.mirror(),False,2))
                        self.assertEqual(fs,codec.features(board.transform(chess.flip_horizontal),True,2))

    def test_pair_cardinality_and_both_perspectives(self):
        board=chess.Board()
        white=codec.features(board,True,2);black=codec.features(board,False,2)
        self.assertEqual(len(white),len(black));self.assertEqual(len(set(white)),len(white))
        # Eight same-file opposite-colour pairs, and four pairs for each adjacent file.
        self.assertEqual(32+8+7*4,len(white))
        self.assertEqual(codec.base_features(board,True),[x for x in white if x<7680])


if __name__=='__main__':unittest.main()
