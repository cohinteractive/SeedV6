// Deterministic wire-format oracle. Build outside the repository with upstream
// data_loader/cpp/lib/{binpack,chess,nodchip,training_data_entry,arithmetic}.h
// from official-stockfish/nnue-pytorch commit 13b44569e0674fe7267122396ea7bd0946f10115.
// cl /EHsc /std:c++20 /I <headers> generate.cpp ; generate.exe <output-directory>
// Generated fixtures contain synthetic positions, never downloaded corpus data.
#include "binpack.h"
#include <sstream>

int main(int argc, char** argv) {
    std::string root = argv[1];
    binpack::CompressedTrainingDataEntryWriter writer(root + "/oracle.binpack", std::ios::out | std::ios::trunc);
    std::ofstream expected(root + "/oracle.tsv", std::ios::binary);
    int n = 0;
    auto sequence = [&](const char* fen, const char* moves) {
        auto pos = chess::Position::fromFen(fen);
        std::istringstream tokens(moves); std::string uci;
        int result = 1;
        while (tokens >> uci) {
            auto move = chess::uci::uciToMove(pos, uci);
            int scores[] = {32002, 100, -100, 0, -32000, 32000, -32768, 32767, 1, -1};
            binpack::TrainingDataEntry e{pos, move, (std::int16_t)scores[n++ % 10], (std::uint16_t)pos.ply(), (std::int16_t)result};
            writer.addTrainingDataEntry(e);
            expected << pos.fen() << '\t' << e.score << '\t' << e.ply << '\t' << e.result << '\t'
                     << int(chess::ordinal(move.from)) << '\t' << int(chess::ordinal(move.to)) << '\t' << int(chess::ordinal(move.type)) << '\n';
            (void)pos.doMove(move); result = -result;
        }
    };
    sequence("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
             "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7 f1e1 b7b5 a4b3 d7d6 c2c3 e8g8 h2h3 c8b7 d2d4 e5d4 c3d4");
    sequence("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 97 30", "e1c1 e8g8 d1d2 f8f7 d2d3");
    sequence("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 150 30", "e8c8 e1g1 d8d7 f1f2");
    sequence("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 20", "e5d6 e8f8 e1f1 f8g8");
    sequence("4k3/8/8/8/3p4/8/4P3/4K3 w - - 0 1", "e2e4 d4e3 e1f1 e8f8");
    sequence("4k3/3p4/8/4P3/8/8/8/4K3 b - - 0 1", "d7d5 e5d6 e8f8 e1f1");
    // Double push beside a pinned pawn: EP must be cleared, just as upstream does.
    sequence("3k4/8/8/8/3p4/8/4P3/K2R4 w - - 12 1", "e2e4 d4d3 a1b1 d3d2");
    sequence("4k3/P7/8/8/8/8/7p/4K3 w - - 0 1", "a7a8q h2h1n a8a7 e8d8");
    sequence("4k3/P7/8/8/8/8/7p/4K3 b - - 0 1", "h2h1q a7a8r h1h2 e1d1");
    sequence("1r2k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7b8b e8f8 b8c7 f8g8");
    sequence("4k3/P7/8/8/8/8/8/4K3 w - - 0 1", "a7a8n e8f8 a8b6 f8g8");
}
