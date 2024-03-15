import pstats
p = pstats.Stats('a1test_parser.prof')
p.print_stats(10)
p.sort_stats('ncalls', 'tottime', 'cumtime').print_stats(10)